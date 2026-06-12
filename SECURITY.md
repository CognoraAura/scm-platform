# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest (`master`) | ✅ Security updates applied |
| Previous release | ⚠️ Critical fixes only for 90 days after a new release |
| Older versions | ❌ No longer supported |

We recommend always running the latest version of SCM Platform.

## Reporting a Vulnerability

**Do NOT report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability, please report it responsibly:

### Contact

- **Email:** [security@scmcloud.com](mailto:security@scmcloud.com)
- **PGP Key:** Available on request for encrypted communications

### What to Include

Please provide as much of the following information as possible:

1. **Type of vulnerability** (e.g., SQL injection, XSS, authentication bypass, authorization bypass)
2. **Affected component** (e.g., `scm-auth`, `scm-order/service`, `scm-gateway`)
3. **Steps to reproduce** — A minimal proof-of-concept or reproduction steps
4. **Potential impact** — What an attacker could achieve
5. **Affected version** — Git commit hash or release tag
6. **Suggested fix** — If you have one (optional)

### Response Timeline

| Phase | Timeline | Description |
|-------|----------|-------------|
| **Acknowledgment** | Within 48 hours | We confirm receipt of your report |
| **Triage** | Within 7 days | We assess severity and confirm the vulnerability |
| **Fix development** | Within 30 days | We develop and test a fix |
| **Release** | Coordinated disclosure | Fix is released, advisory is published |

We will keep you informed of our progress throughout the process. If a fix requires more time, we will communicate the revised timeline.

## Security Features

SCM Platform implements multiple layers of security:

### Authentication & Authorization

- **OAuth 2.0** — Standard authorization framework with JWT access tokens
- **WebAuthn** — Passwordless authentication support (FIDO2)
- **RBAC** — Role-based access control with fine-grained data scope control
- **Multi-tenant isolation** — Tenant data is isolated at the database level via dynamic data source routing

### Transport Security

- **TLS/SSL** — All services communicate over TLS
- **mTLS** — Mutual TLS for inter-service communication (configurable)
- **API Gateway** — Centralized security enforcement at `scm-gateway`

### API Security

- **API signatures** — Request signing to prevent tampering
- **Rate limiting** — Sentinel-based rate limiting and circuit breaking
- **Input validation** — Server-side validation on all inputs

### Data Security

- **SQL injection prevention** — Parameterized queries via MyBatis-Plus, no raw SQL concatenation
- **XSS prevention** — Output encoding and Content-Security-Policy headers
- **CSRF protection** — Token-based CSRF prevention
- **Password hashing** — BCrypt with configurable strength
- **Sensitive data encryption** — AES encryption for sensitive fields at rest

### Infrastructure

- **Dependency scanning** — OWASP Dependency Check in CI pipeline
- **Static analysis** — SonarCloud integration for code quality and security
- **Container scanning** — Docker image scanning in CI/CD

## Responsible Disclosure

We believe in responsible disclosure and commit to the following:

### Our Commitments

- We will acknowledge your contribution in the [SECURITY.md](./SECURITY.md) acknowledgments section (with your permission)
- We will not take legal action against researchers who make a good-faith effort to follow this policy
- We will work with you to understand and resolve the issue promptly
- We will credit you in the security advisory (unless you prefer to remain anonymous)

### Researcher Guidelines

When researching vulnerabilities, please:

- **Do** make a good-faith effort to avoid data destruction, service disruption, and privacy violations
- **Do** stop testing once you have confirmed a vulnerability and report it immediately
- **Do** only test against your own local instance or explicitly authorized environments
- **Do** provide reasonable time for us to address the issue before public disclosure

Please **do not**:

- Access, modify, or delete data belonging to other users
- Perform denial-of-service attacks
- Conduct social engineering attacks against our team or users
- Disclose the vulnerability publicly before a fix is available
- Use automated scanners that generate excessive traffic against production systems

## Scope

### In Scope

The following are considered valid security vulnerabilities:

| Category | Examples |
|----------|----------|
| **Authentication bypass** | Skipping login, forging tokens, session fixation |
| **Authorization bypass** | Accessing other tenants' data, privilege escalation |
| **SQL injection** | Injecting SQL through any input field or API parameter |
| **Cross-site scripting (XSS)** | Stored, reflected, or DOM-based XSS |
| **Cross-site request forgery (CSRF)** | State-changing requests without proper CSRF protection |
| **Data leakage** | Exposure of sensitive data (passwords, tokens, PII) through errors, logs, or APIs |
| **Remote code execution** | Arbitrary code execution on the server |
| **Insecure deserialization** | Exploiting Java deserialization vulnerabilities |
| **Server-side request forgery (SSRF)** | Making the server issue requests to internal resources |
| **Cryptographic weaknesses** | Weak encryption, predictable random values, hardcoded keys |

### Out of Scope

The following are **not** considered security vulnerabilities:

| Category | Reason |
|----------|--------|
| **Social engineering** | Attacks targeting people, not systems |
| **Denial of service (DoS/DDoS)** | Availability attacks, resource exhaustion |
| **Outdated dependencies** | Vulnerabilities in dependencies with known fixes (please report, but these are lower priority) |
| **Issues in third-party services** | Vulnerabilities in Nacos, Redis, PostgreSQL, etc. themselves |
| **Self-XSS** | XSS that only affects the user performing the injection |
| **Missing best practices** | Security hardening suggestions without a concrete exploit |
| **Issues requiring physical access** | Vulnerabilities requiring physical access to the server |
| **Issues in non-production environments** | Vulnerabilities in development or test instances |

## Security Advisories

When a security fix is released, we publish a security advisory on GitHub with:

- Description of the vulnerability
- Affected versions
- Severity rating (CVSS score)
- Mitigation steps
- Credit to the reporter (with permission)

## Acknowledgments

We thank the following researchers for responsibly disclosing security vulnerabilities:

*No vulnerabilities reported yet. Be the first to help us improve!*

---

For security questions or concerns, contact [security@scmcloud.com](mailto:security@scmcloud.com).
