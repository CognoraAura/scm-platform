# Design Philosophy

> *Opinions are architecture. Every technical decision is a value judgment in disguise.*

---

## Preamble

This document does not describe what the system does. The architecture document covers that.

This document explains what we believe — about distributed systems, about collaboration, about the relationship between AI and creative workflows, and about what "correct" means when correctness is not binary.

These beliefs shaped every design decision. Where we chose differently than the consensus, we explain why. Where the tradeoffs were painful, we say so.

---

## I. Collaboration Is the Primary Abstraction

Most systems treat collaboration as a layer added on top of a storage system. You upload a file, the system stores it, and collaboration is grafted on — comments here, version history there, real-time cursors as a premium feature.

This system inverts that. Collaboration is the primary abstraction. Storage is the thing underneath it.

The difference is not cosmetic. When collaboration is primary, the data model is shaped around concurrent access from the start. The consistency model is designed for multi-writer scenarios before the single-writer case. The network protocol is chosen for low-latency bidirectional communication, not request-response.

When collaboration is added later, you get locks, optimistic concurrency errors surfaced to users, and "someone else is editing this" warnings. These are not UX failures — they are architectural failures that have leaked into the interface.

**The inversion means:** a single user uploading an asset in isolation is a degenerate case of the collaboration model, not the primary case that the collaboration model is bolted onto.

---

## II. Why Yjs CRDT — and Why Not OT

The choice of synchronization primitive is the most consequential decision in any collaborative system. Get it wrong and you spend years fighting your consistency model.

### Rejected: Operational Transformation

OT was the standard for decades. Google Docs uses it. Its central claim is correct: you can represent concurrent edits as operations, transform those operations relative to each other, and arrive at a consistent state.

The problem is the "transform" step. For text, the transformation functions are well-understood. For rich structured data — tagged assets, nested metadata, multi-field records — transformation functions are combinatorially complex and notoriously difficult to prove correct. The literature is full of OT algorithms that are subtly broken under specific concurrent edit patterns.

More fundamentally: OT requires a central server to maintain a canonical operation ordering. That server is a coordination bottleneck. Every client operation must pass through it before being applied. Network partitions become correctness failures, not just availability failures.

### Chosen: Yjs CRDT

CRDTs make a different trade. Instead of transforming operations, they choose data structures that are closed under a commutative, associative, idempotent merge function. Any two replicas can merge independently and arrive at the same state, regardless of operation order.

This has three consequences that matter for this system:

**Offline resilience.** A client can accumulate edits while disconnected and merge them on reconnect without coordination. The system does not need to know that the client was offline — the merge function handles it transparently.

**Peer-to-peer capability.** The server is a relay and persistence layer, not a merge authority. Two clients could, in principle, exchange updates directly. The architecture doesn't use this, but the property constrains us toward better designs.

**Mathematical correctness.** Convergence is a property of the data structure, not a property of the implementation. You cannot write a bug that causes divergence without breaking the CRDT invariants.

### The Cost

Yjs document state is binary and opaque. Inspecting document state in a database console requires a deserializer. Debugging merge behavior requires understanding the Yjs internal encoding. This is a real operational cost that teams consistently underestimate.

Server-side CRDT operations — merging incoming updates into the persisted document state — require either a Java CRDT implementation or a runtime capable of executing the JavaScript Yjs library. We use GraalVM Polyglot for the latter. This adds JVM startup complexity and a dependency on a non-trivial runtime feature.

We accepted these costs. The alternative — a system where concurrent edits occasionally lose data or require user intervention to resolve — is not acceptable at any scale.

---

## III. Event-Driven Synchronization — Philosophy, Not Pattern

Event-driven architecture is often adopted as a pattern: "we should decouple these services." That framing is too shallow. We adopted it as a philosophy about what the system should be able to observe, replay, and evolve.

### The Core Claim

A system that communicates exclusively through synchronous method calls is a system where every operation must complete before the next one can start, where every consumer of a result must be known at call time, and where adding a new consumer requires modifying the producer.

A system that communicates through events is a system where operations complete when they are recorded, where consumers discover events rather than being wired to producers, and where the event log is a first-class artifact that can be replayed, audited, and extended.

For asset workflows, this matters because:

- An uploaded asset triggers tagging, deduplication, moderation, thumbnail generation, search indexing, and potentially notifications. Doing these synchronously would make uploads slow, fragile, and tightly coupled.
- The set of things that happen on asset ingest is not fixed. It will change. Event-driven systems absorb this change without modifying the upload path.
- Failures in enrichment (AI API timeout, moderation service degradation) should not propagate to the user's upload experience.

### The Event as Boundary

Every domain event is a boundary between what the system knows and what it is doing about it. `AssetIngested` means: the system knows this asset exists durably. What it does about that fact — tagging, deduplication, notifications — is downstream of that knowledge, not part of it.

This is not just organizational hygiene. It is a consistency argument: the upload path makes one strong commitment (the asset is stored) and makes that commitment visible to the rest of the system. Downstream processes operate on that fact, not on the in-progress upload.

### Rejected: Synchronous Pipeline

A synchronous pipeline — upload, then tag, then dedup, then return — gives the user a single wait and a single result. It is simpler to reason about in isolation.

The failure modes are unacceptable. An AI API timeout at 15 seconds becomes the user's upload latency. A moderation service outage blocks all uploads. Adding a new enrichment step requires changing the upload handler and its error handling.

The complexity of asynchrony is real. Events can be lost. Consumers can fail. Ordering is not guaranteed. We address these through idempotent consumers, retryable stages, and a correlation ID that makes causality traceable across asynchronous boundaries. The complexity is managed, not eliminated.

---

## IV. Cache-Aware Media Delivery — First Principles

A cache is not a performance optimization bolted onto a correct system. In a media delivery system, the cache architecture *is* the performance architecture. Getting it wrong means the correct behavior is also the slow behavior.

### Why Two Levels

The latency difference between L1 (JVM heap, ~1µs) and L2 (Redis, ~1ms) is three orders of magnitude. For a system serving thousands of asset URL requests per second, this difference is the difference between spending CPU cycles on serving requests and spending CPU cycles on network I/O to Redis.

L1 exists for the hot set — the top few percent of assets by access frequency. These assets do not pay the Redis RTT. For a creative team working on an active campaign, all the assets they're looking at in the last hour are in this set.

L2 exists for the warm set — assets that are accessed regularly but not so frequently that they justify JVM heap space. Redis is shared across all application pods, so a cache warm from one pod is immediately available to others.

The origin — PostgreSQL and object storage — is touched only on cold misses. For a healthy system, this should be a small percentage of traffic.

### The Invalidation Contract

Cache invalidation is simple in this system because presigned URLs are the cached artifact, not the asset binary. Presigned URLs have short TTLs (15 minutes). The cache TTL (30 minutes for L2) is aligned to the URL expiry with a safety margin.

On asset deletion or version update, both L1 and L2 entries are synchronously invalidated. This is a strong invalidation — the stale window is zero. We pay the cost of a synchronous Redis write on every mutation. For a system where assets are read far more than they are modified, this is the correct tradeoff.

### Write-Around Strategy

We do not pre-warm the cache on upload. Newly ingested assets are not inserted into L1 or L2 at write time. The first read triggers population.

The alternative — write-through — would cause cache churn on batch uploads (a common pattern for creative teams importing an asset library). Batch uploads are read-rarely-after-write: the assets are uploaded, processed, and then selectively opened. Pre-warming the cache for all of them would evict genuinely hot assets in favor of assets that may never be read.

Write-around accepts slightly higher latency on first access in exchange for a cache that reflects actual access patterns rather than upload patterns.

---

## V. Storage Abstraction — The Future-Proofing Argument

Storage backends change. Not because engineers are indecisive, but because the economics change, the compliance requirements change, the deployment targets change, and the vendors change their pricing models.

A system that has Tencent COS SDK calls distributed across its business logic has made a permanent architectural commitment to Tencent COS. Migrating requires finding every call site, understanding the behavioral differences between APIs, and testing every code path against the new backend. This is expensive enough that it often doesn't happen.

The `ObjectStoragePort` interface is a one-time investment that makes future migrations cheap. The interface is small — five methods. The adapters are isolated. The business logic never touches a storage SDK directly.

### What the Interface Encodes

The interface encodes *what* the business logic needs, not *how* a specific backend provides it. `presignedGet(key, ttl)` is a business concept: give me a time-bounded URL for this asset. Whether that URL is signed by COS, S3, or a local file server with a JWT is the adapter's concern.

This matters for local development and self-hosted deployments. The `LocalFileObjectStorageAdapter` implements the same interface as the COS adapter. Tests run against the local adapter. The code path is identical.

### Rejected: Direct SDK Integration

Direct integration is faster to write initially. The COS SDK is good. The documentation is adequate. The temptation is real.

The problem is not the SDK — it is what happens to the system over the first two years of operation. The team that chose COS for cost reasons discovers that their deployment environment requires on-premise storage. The system that has COS assumptions scattered through twenty service methods now requires a significant refactor to support a new target.

The port interface adds perhaps two hours of initial implementation time. It saves weeks of migration work later. The math is not close.

---

## VI. AI as Ingest Infrastructure

The conventional approach to AI features in asset management systems: a user selects some assets, clicks "Generate Tags," waits for a spinner, and reviews the results.

This approach treats AI as a user-invoked tool. It is better than nothing. It is not good enough.

### The Ambient Enrichment Argument

When AI tagging is a user action, you have two problems. First, most assets are never tagged — the action is manual, and manual actions are often skipped. Second, the system has no consistent enrichment baseline — some assets are tagged, some are not, and queries that depend on tags have unpredictable recall.

When AI tagging is part of the ingest contract — an unconditional pipeline stage that runs on every asset — you have a different system. Every asset is tagged. The search index has consistent coverage. Deduplication signals are available for every upload. Content policy checks run before assets appear in the UI.

The user sees better results without taking any additional action. This is what "AI as infrastructure" means.

### The Async Contract

AI processing is not fast enough to be synchronous on the upload path. Qwen-VL inference for a high-resolution image can take seconds. Users should not wait seconds for an upload acknowledgment.

The async pipeline is the correct structure: the upload returns immediately when storage is durable, and enrichment runs in the background. The client polls or receives a push notification when enrichment is complete. The asset is usable immediately; it is fully enriched within seconds.

### Taxonomy Normalization

Raw AI output is not directly usable in a multi-tenant system. Qwen-VL returns "outdoor photograph" and "landscape." Different teams have different taxonomies — one uses "exterior," another uses "nature," a third has a strict controlled vocabulary for brand asset classification.

Taxonomy normalization happens at the pipeline boundary, not in the AI call. The AI produces raw semantic labels. The normalization stage maps them to the tenant's vocabulary. This separation means the AI call is tenant-agnostic — the same model serves all tenants — while the output is tenant-specific.

---

## VII. Multi-Tenant Isolation — Defense in Depth

Multi-tenancy is not a feature. It is a security property. The consequences of getting it wrong are not performance degradation or a confusing UI — they are data breaches, compliance failures, and loss of customer trust that cannot be recovered.

### The Threat Model

The threat we are defending against is not a sophisticated attacker who has compromised the application server. It is an application bug — a missing filter clause, a misconfigured query, a shared cache key — that allows one tenant's request to return another tenant's data.

These bugs are not theoretical. They are the most common multi-tenancy failure mode. They are also the hardest to catch in testing, because they typically require two tenants with overlapping data to be exercising the system simultaneously.

### Defense at the Persistence Layer

The primary defense is a Hibernate filter that adds `WHERE tenant_id = ?` to every query on tenant-scoped tables. The filter is activated at session creation and cannot be disabled from application code. It is not a convention — it is a constraint enforced by the framework.

This means a missing filter clause in a repository method does not return another tenant's data — it returns an error (or returns nothing, because the filter is still applied). The defense is in the infrastructure, not in the developer's attention.

### Defense at the Storage Layer

Storage keys are prefixed with the tenant ID: `{tenantId}/{year}/{month}/{assetId}/{variant}`. This provides storage-level isolation — a signed URL for tenant A's asset cannot be used to access tenant B's asset, because the URL encodes the tenant prefix.

This also enables per-tenant cost attribution at the storage billing level, without querying the database.

### The Schema-Per-Tenant Option

For enterprise tenants with regulatory requirements (data residency, compliance audits requiring physical data separation), the system supports schema-per-tenant isolation via a routing DataSource. The application code is identical — the routing happens at the JDBC connection level.

This is not the default because it adds operational complexity: more schema migration paths, more connection pool management, more monitoring surface area. It is available for the cases where the compliance requirement is real.

---

## VIII. What This System Does Not Solve

A system's non-goals are as important as its goals. A system that tries to solve everything solves nothing well.

**This system does not solve video processing.** Video is a different problem domain — transcoding pipelines, streaming protocols, adaptive bitrate delivery. Adding video support to this system would mean adding a media processing infrastructure that dwarfs the existing system in complexity. The asset pipeline is designed for still images and can be extended to document types. Video requires a different architecture.

**This system does not solve global strong consistency.** The consistency model is deliberately mixed — strong where correctness requires it, eventual where latency matters more. A system that requires global strong consistency across all operations cannot achieve the latency targets that make real-time collaboration feel real. We chose the consistency level appropriate to each operation.

**This system does not solve cross-region replication at the application layer.** The storage abstraction supports a multi-region-capable backend. The application does not implement region-aware routing or conflict resolution across region replicas. Cross-region replication is a deployment decision, made at the infrastructure layer, not a feature of the application.

**This system does not solve public anonymous delivery.** Presigned URLs provide authenticated, time-bounded access. Building a public CDN delivery system — with anonymous access, edge caching, and cache invalidation at CDN nodes — is a different product with different security properties. Teams that need public asset hosting use a CDN in front of the presigned URL layer.

**This system does not solve pixel-level collaborative drawing.** Yjs-backed text and metadata editing converges correctly for the field types in scope. Collaborative canvas drawing — where the document is a pixel buffer or a vector scene graph — requires a different CRDT structure and a different rendering model. This is Figma's problem. This system solves the asset management problem adjacent to it.

---

## IX. Scaling Philosophy

Scale is not a feature you add. It is a property of the architecture — either the design allows horizontal scaling or it does not, and retrofitting scalability into a design that assumed a single process is expensive.

### What Must Scale

**The HTTP tier scales horizontally by design.** Stateless pods, JWT-carried identity, no shared in-process state. Adding a pod requires no coordination. This is not accidental — it is the result of never storing anything that matters in the application process.

**The WebSocket tier is the harder problem.** STOMP connections are stateful — a client connected to pod A cannot receive broadcasts from pod B without a pub/sub layer between them. Redis pub/sub serves as the cross-pod broadcast mechanism for CRDT update distribution. This introduces a Redis availability dependency on the collaboration path, which is a deliberate tradeoff against the complexity of a fully distributed WebSocket tier.

**The AI pipeline scales independently.** Workers are stateless consumers of domain events. Adding workers adds throughput. The event bus is the backpressure mechanism — if workers fall behind, events queue. This is acceptable because AI enrichment is asynchronous and not on the critical user path.

### What Does Not Need to Scale Immediately

**The event bus is in-process by default.** This is correct for single-server and small multi-server deployments. The `DomainEventPublisher` interface is the scaling seam — when event volume requires a distributed bus, the implementation changes, not the business logic.

**PostgreSQL is not sharded.** For most deployments, a well-tuned PostgreSQL instance with read replicas handles the load. Sharding adds complexity that is unjustified until the single-instance ceiling is reached. We build toward that ceiling before crossing it.

### The Principle

Scale on demand, not in advance. Every premature scaling decision adds complexity that must be maintained, debugged, and explained to new team members. Add complexity when the problem is demonstrated, not when it is anticipated.

---

## X. Operational Philosophy

A system that is correct but unoperatable is not correct in production. Operational concerns are not second-class — they are part of the design.

### Observability Is Not Optional

Structured logs, distributed traces, and business metrics are not added after the system works. They are written alongside the features they observe. A feature that cannot be observed is not complete.

Every asynchronous operation carries a correlation ID derived from the originating request. This makes it possible to answer "why did this upload take 30 seconds?" by following the correlation ID through the logs of the upload handler, the event bus, and every pipeline worker — without searching for needle-in-haystack patterns in unstructured log lines.

### Idempotency Is a Contract

Every pipeline stage is idempotent. Running a stage twice on the same asset produces the same result as running it once. This is not a performance property — it is a correctness property that enables retries.

In a distributed system, at-least-once delivery is easier to achieve than exactly-once. We design for at-least-once by making consumers correct under repeated delivery. The alternative — exactly-once delivery — requires distributed transactions that add latency and complexity that is rarely worth the cost.

### Failure Is Normal

The system is designed to fail gracefully, not to prevent failure. External AI APIs will timeout. Object storage will return occasional errors. Redis will occasionally be unavailable.

The response to each of these is specified in the design: AI API timeout → retry with exponential backoff, fall back to untagged. Storage error → fail the upload, return error to client, do not emit the domain event. Redis unavailable → fall through to PostgreSQL for cache misses, degrade gracefully on WebSocket broadcast.

A system that has not specified its failure behavior has not finished its design.

---

## Closing

Every opinion in this document is falsifiable. We believe that CRDT convergence is worth the operational cost of binary document state. We believe that write-around cache strategy is correct for upload-heavy creative workflows. We believe that event-driven async pipelines are worth the debugging complexity they introduce.

If these beliefs are wrong for your deployment context, the architecture tells you where to change them. The port interfaces are the seams. The event bus is replaceable. The cache strategy is configurable.

Good architecture is opinionated and evolvable. Opinions that cannot be revised are dogma, not design.

---

*This document reflects the beliefs of the team at the time of writing. Beliefs should change when evidence changes.*
