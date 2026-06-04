# SCM Platform — Frontend Architecture & Development Planning Document

> **Version:** 1.0  
> **Date:** 2026-06-04  
> **Status:** Draft  
> **Author:** Frontend Architecture Team  
> **Audience:** Frontend Engineers, Tech Leads, Architects

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Folder Structure](#2-folder-structure)
3. [Layered Architecture](#3-layered-architecture)
4. [Feature-Based Module Structure](#4-feature-based-module-structure)
5. [Authentication Architecture](#5-authentication-architecture)
6. [Routing Architecture](#6-routing-architecture)
7. [API Layer Design](#7-api-layer-design)
8. [TanStack Query Architecture](#8-tanstack-query-architecture)
9. [State Management](#9-state-management)
10. [Form Architecture](#10-form-architecture)
11. [Shared Components System](#11-shared-components-system)
12. [Table Architecture](#12-table-architecture)
13. [Theme System](#13-theme-system)
14. [Internationalization](#14-internationalization)
15. [Real-time Architecture](#15-real-time-architecture)
16. [Dashboard Architecture](#16-dashboard-architecture)
17. [File Management Architecture](#17-file-management-architecture)
18. [Error Handling Strategy](#18-error-handling-strategy)
19. [Performance Optimization](#19-performance-optimization)
20. [Security Design](#20-security-design)
21. [Testing Strategy](#21-testing-strategy)
22. [CI/CD](#22-cicd)
23. [Frontend Observability](#23-frontend-observability)
24. [Development Standards](#24-development-standards)
25. [Dependency Management](#25-dependency-management)
26. [Development Roadmap](#26-development-roadmap)
27. [Task Breakdown](#27-task-breakdown)
28. [Future Evolution](#28-future-evolution)

---

## 1. Architecture Overview

### 1.1 Technology Choices & Rationale

#### Why React + Next.js (App Router)

| Factor | Rationale |
|--------|-----------|
| **Ecosystem maturity** | React has the largest component ecosystem; Ant Design Pro is React-native |
| **App Router** | File-based routing with nested layouts, loading states, error boundaries — reduces boilerplate by ~40% |
| **Server Components** | Reduce client bundle size; fetch data on server for initial renders (dashboard KPIs, list pages) |
| **Server Actions** | Type-safe mutations without API routes for simple cases |
| **Streaming SSR** | Progressive page rendering improves TTFB for data-heavy pages |
| **Parallel routes** | Dashboard panels, modal routes, concurrent data loading |
| **Middleware** | Auth guards, tenant routing, locale detection at the edge |

**ADR-001:** Next.js App Router over Pages Router — App Router provides nested layouts (critical for admin shell), React Server Components, and streaming. Pages Router lacks these primitives.

**ADR-002:** React over Vue — Ant Design Pro Components ecosystem is React-native; team expertise; TanStack Query has superior React integration.

#### Why Ant Design Pro Components

| Component | Use Case |
|-----------|----------|
| `ProTable` | All CRUD tables with search, pagination, column settings |
| `ProForm` | All forms with validation, wizard forms, modal forms |
| `ProLayout` | Admin shell with sidebar, breadcrumbs, tabs |
| `ProCard` | Dashboard cards, statistics panels |
| `ProDescriptions` | Detail pages, read-only data display |
| `ProList` | List views with actions |
| `ProSkeleton` | Loading states |

**ADR-003:** Ant Design Pro over raw Ant Design — Pro components handle 80% of admin UI patterns (tables, forms, layouts) with minimal configuration. Raw Ant Design requires building these patterns from scratch.

#### Why TanStack Query Instead of Redux for Server State

| Concern | TanStack Query | Redux Toolkit |
|---------|---------------|---------------|
| Server state caching | Built-in with stale/fresh lifecycle | Manual cache management |
| Background refetch | Automatic | Manual |
| Optimistic updates | First-class support | Manual implementation |
| Pagination/infinite | `useInfiniteQuery` built-in | Custom implementation |
| Devtools | Dedicated query devtools | Redux DevTools |
| Boilerplate | Minimal (~5 lines per query) | Slices, thunks, selectors (~30 lines) |
| Bundle size | ~13KB | ~11KB + RTK Query ~22KB |

**ADR-004:** TanStack Query for all server/API state. Redux is unnecessary when server state is separated from client state. TanStack Query eliminates cache synchronization bugs that plague Redux-based apps.

#### Why Zustand for Client State

| Factor | Zustand | Redux Toolkit | Jotai |
|--------|---------|---------------|-------|
| Bundle size | ~1.1KB | ~11KB | ~3KB |
| Boilerplate | Minimal | Moderate | Minimal |
| Devtools | Supported | Excellent | Limited |
| Persistence | `zustand/middleware` built-in | `redux-persist` | Manual |
| TypeScript | Excellent | Good | Good |
| Learning curve | Low | Medium | Low |

**ADR-005:** Zustand for UI/client state (theme, sidebar, preferences). It's tiny, has zero boilerplate, and works outside React components (useful for interceptors accessing auth state).

### 1.2 SSR vs CSR Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Rendering Strategy                        │
├─────────────────┬──────────────────┬────────────────────────┤
│   SSR / RSC     │   CSR with       │   Full CSR             │
│   (Initial)     │   Hydration      │   (No SSR)             │
├─────────────────┼──────────────────┼────────────────────────┤
│ Dashboard       │ Form pages       │ Monaco Editor          │
│ List pages      │ Detail pages     │ ReactFlow diagrams     │
│ Login page      │ Settings pages   │ ECharts (heavy)        │
│ Landing pages   │                  │ Real-time dashboards   │
└─────────────────┴──────────────────┴────────────────────────┘
```

**Strategy:**
- **Server Components (default):** Layouts, static content, initial data fetching for lists/dashboards
- **Client Components (`'use client'`):** Interactive forms, tables with real-time updates, editors, charts
- **Suspense boundaries:** Wrap each major content area for progressive loading

### 1.3 Overall Architecture Principles

1. **Feature-based organization** — Group by business domain, not technical layer
2. **Server-first rendering** — Use RSC where possible, opt into client only when needed
3. **Type safety end-to-end** — OpenAPI generated types → Zod schemas → React Hook Form
4. **Convention over configuration** — File-based routing, naming conventions, barrel exports
5. **Progressive enhancement** — Core functionality works without JavaScript; enhanced with client interactivity
6. **Separation of concerns** — Server state (TanStack Query) ≠ Client state (Zustand) ≠ Form state (React Hook Form)

### 1.4 Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Browser                                      │
├──────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                     Next.js App Router                          │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │ │
│  │  │  Pages   │ │ Layouts  │ │ Loading  │ │  Error Boundaries│  │ │
│  │  │  (RSC)   │ │  (RSC)   │ │  (RSC)   │ │   (Client)       │  │ │
│  │  └────┬─────┘ └──────────┘ └──────────┘ └──────────────────┘  │ │
│  │       │                                                         │ │
│  │  ┌────▼─────────────────────────────────────────────────────┐  │ │
│  │  │                 Feature Modules                           │  │ │
│  │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐   │  │ │
│  │  │  │Dashboard│ │ Product │ │  Order  │ │  Inventory  │   │  │ │
│  │  │  └────┬────┘ └────┬────┘ └────┬────┘ └──────┬──────┘   │  │ │
│  │  └───────┼───────────┼───────────┼──────────────┼──────────┘  │ │
│  │          │           │           │              │              │ │
│  │  ┌───────▼───────────▼───────────▼──────────────▼──────────┐  │ │
│  │  │              Application Layer                            │  │ │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │  │ │
│  │  │  │  Hooks   │ │  Stores  │ │ Services │ │  Schemas   │ │  │ │
│  │  │  │(TanStack │ │ (Zustand)│ │ (Axios)  │ │   (Zod)    │ │  │ │
│  │  │  │  Query)  │ │          │ │          │ │            │ │  │ │
│  │  │  └──────────┘ └──────────┘ └────┬─────┘ └────────────┘ │  │ │
│  │  └─────────────────────────────────┼───────────────────────┘  │ │
│  │                                    │                           │ │
│  │  ┌─────────────────────────────────▼───────────────────────┐  │ │
│  │  │              API Layer (Axios + Interceptors)            │  │ │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │  │ │
│  │  │  │  Auth    │ │  Retry   │ │  Error   │ │  Tenant    │ │  │ │
│  │  │  │Interceptor│ │Strategy │ │ Handler  │ │  Header    │ │  │ │
│  │  │  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │  │ │
│  │  └─────────────────────────────────┬───────────────────────┘  │ │
│  └────────────────────────────────────┼──────────────────────────┘ │
│                                       │                             │
└───────────────────────────────────────┼─────────────────────────────┘
                                        │ HTTPS
                                        ▼
                              ┌─────────────────┐
                              │  API Gateway    │ :8761
                              │  (Spring Cloud) │
                              └─────────────────┘
```

---

## 2. Folder Structure

### 2.1 Root Structure

```
scm-frontend/
├── .github/                    # CI/CD workflows
│   └── workflows/
│       ├── ci.yml
│       ├── deploy.yml
│       └── e2e.yml
├── .husky/                     # Git hooks
│   ├── pre-commit
│   └── commit-msg
├── .opencode/                  # OpenCode config
├── app/                        # Next.js App Router (pages & layouts)
├── components/                 # Shared UI components
├── features/                   # Feature modules (business logic)
├── hooks/                      # Shared custom hooks
├── stores/                     # Zustand stores
├── lib/                        # Third-party library configurations
├── generated/                  # OpenAPI generated types & clients
├── services/                   # API service layer
├── types/                      # Shared TypeScript types
├── constants/                  # Application constants
├── schemas/                    # Shared Zod schemas
├── providers/                  # React context providers
├── layouts/                    # Layout components
├── messages/                   # i18n translation files
├── public/                     # Static assets
├── scripts/                    # Build & utility scripts
├── __tests__/                  # Global test utilities
├── .env.example                # Environment variables template
├── .env.local                  # Local environment (gitignored)
├── .eslintrc.json              # ESLint config
├── .prettierrc                 # Prettier config
├── components.json             # shadcn/ui config (optional)
├── docker-compose.yml          # Local dev environment
├── Dockerfile                  # Production build
├── next.config.ts              # Next.js configuration
├── openapitools.json           # OpenAPI generator config
├── package.json
├── playwright.config.ts        # E2E test config
├── postcss.config.mjs
├── tailwind.config.ts          # Tailwind (if used alongside Ant Design)
├── tsconfig.json               # TypeScript strict config
└── vitest.config.ts            # Unit test config
```

### 2.2 Detailed Folder Responsibilities

#### `app/` — Next.js App Router

```
app/
├── (auth)/                     # Auth route group (public)
│   ├── login/
│   │   └── page.tsx
│   ├── register/
│   │   └── page.tsx
│   └── layout.tsx              # Auth layout (no sidebar)
├── (dashboard)/                # Main app route group (protected)
│   ├── layout.tsx              # Dashboard layout (sidebar + header)
│   ├── loading.tsx             # Global loading skeleton
│   ├── error.tsx               # Global error boundary
│   ├── not-found.tsx           # 404 page
│   ├── dashboard/
│   │   └── page.tsx
│   ├── product/
│   │   ├── page.tsx            # Product list
│   │   ├── [id]/
│   │   │   └── page.tsx        # Product detail
│   │   └── create/
│   │       └── page.tsx        # Create product
│   ├── order/
│   ├── inventory/
│   ├── warehouse/
│   ├── purchase/
│   ├── supplier/
│   ├── logistics/
│   ├── finance/
│   ├── tenant/
│   ├── system/
│   │   ├── user/
│   │   ├── role/
│   │   ├── permission/
│   │   └── dictionary/
│   └── notification/
├── api/                        # API routes (BFF if needed)
│   └── auth/
│       └── [...nextauth]/
├── [locale]/                   # Locale routing (zh-CN, en)
│   └── (dashboard)/
│       └── ...
├── layout.tsx                  # Root layout
├── globals.css                 # Global styles
├── template.tsx                # Template (re-renders on navigation)
└── middleware.ts                # Auth + locale middleware
```

**Responsibility:** File-based routing, layouts, loading/error states, metadata.

#### `components/` — Shared UI Components

```
components/
├── ui/                         # Primitive UI components
│   ├── button.tsx
│   ├── input.tsx
│   ├── select.tsx
│   ├── date-picker.tsx
│   ├── modal.tsx
│   ├── drawer.tsx
│   ├── notification.tsx
│   └── index.ts                # Barrel export
├── business/                   # Business-specific shared components
│   ├── tenant-switcher.tsx
│   ├── user-avatar.tsx
│   ├── status-tag.tsx
│   ├── audit-timeline.tsx
│   └── approval-flow.tsx
├── table/                      # Table utilities
│   ├── pro-table-wrapper.tsx
│   ├── column-settings.tsx
│   ├── export-button.tsx
│   └── batch-actions.tsx
├── form/                       # Form utilities
│   ├── pro-form-wrapper.tsx
│   ├── form-modal.tsx
│   ├── form-drawer.tsx
│   └── wizard-form.tsx
├── upload/                     # Upload components
│   ├── file-upload.tsx
│   ├── image-upload.tsx
│   ├── chunk-upload.tsx
│   └── excel-import.tsx
├── charts/                     # Chart wrappers
│   ├── line-chart.tsx
│   ├── bar-chart.tsx
│   ├── pie-chart.tsx
│   ├── gauge-chart.tsx
│   └── chart-container.tsx
├── editor/                     # Code/content editors
│   ├── monaco-editor.tsx
│   └── markdown-editor.tsx
├── flow/                       # Flow diagrams
│   ├── workflow-designer.tsx
│   └── approval-flow-viewer.tsx
├── layout/                     # Layout components
│   ├── admin-shell.tsx
│   ├── sidebar.tsx
│   ├── header.tsx
│   ├── breadcrumb.tsx
│   └── tab-bar.tsx
└── index.ts                    # Barrel export
```

**Responsibility:** Reusable, framework-agnostic UI building blocks. No business logic.

#### `features/` — Feature Modules

```
features/
├── dashboard/
├── product/
├── inventory/
├── warehouse/
├── order/
├── purchase/
├── supplier/
├── logistics/
├── finance/
├── tenant/
├── system/
├── dictionary/
└── notification/
```

**Responsibility:** Business-domain-specific code. Each module is self-contained. See Section 4 for details.

#### `hooks/` — Shared Custom Hooks

```
hooks/
├── use-api.ts                  # Generic API hook wrapper
├── use-auth.ts                 # Authentication state
├── use-debounce.ts             # Debounce utility
├── use-local-storage.ts        # Local storage with SSR safety
├── use-media-query.ts          # Responsive breakpoints
├── use-permission.ts           # RBAC permission checks
├── use-tenant.ts               # Current tenant context
├── use-websocket.ts            # WebSocket connection
├── use-breakpoint.ts           # Ant Design responsive
└── index.ts
```

**Responsibility:** Reusable React hooks that are not tied to a specific feature.

#### `stores/` — Zustand Stores

```
stores/
├── auth-store.ts               # Auth tokens, user session
├── ui-store.ts                 # Sidebar, theme, density
├── tenant-store.ts             # Current tenant, tenant list
├── notification-store.ts       # Real-time notifications
├── preference-store.ts         # User preferences (persisted)
└── index.ts
```

**Responsibility:** Client-side state that persists across components. See Section 9.

#### `lib/` — Library Configurations

```
lib/
├── axios.ts                    # Axios instance & interceptors
├── tanstack-query.ts           # QueryClient configuration
├── dayjs.ts                    # Day.js plugins & locale
├── echarts.ts                  # ECharts theme & registration
├── stomp.ts                    # STOMP WebSocket client
├── i18n.ts                     # next-intl configuration
└── utils.ts                    # General utilities (clsx, cn, etc.)
```

**Responsibility:** Third-party library setup and configuration.

#### `generated/` — OpenAPI Generated Code

```
generated/
├── api/                        # Generated API clients
│   ├── order-api.ts
│   ├── product-api.ts
│   ├── inventory-api.ts
│   └── ...
├── models/                     # Generated TypeScript interfaces
│   ├── order.ts
│   ├── product.ts
│   └── ...
└── index.ts                    # Barrel export
```

**Responsibility:** Auto-generated types and API clients from OpenAPI specs. Never hand-edit.

#### `services/` — API Service Layer

```
services/
├── auth.service.ts             # Auth API calls
├── order.service.ts            # Order API calls
├── product.service.ts          # Product API calls
├── upload.service.ts           # File upload API
└── index.ts
```

**Responsibility:** Thin wrappers around generated API clients. Add request/response transformation, error mapping.

#### `types/` — Shared TypeScript Types

```
types/
├── api.ts                      # API response types
├── auth.ts                     # Auth-related types
├── common.ts                   # Common types (pagination, etc.)
├── route.ts                    # Route metadata types
├── theme.ts                    # Theme token types
└── index.ts
```

**Responsibility:** Shared type definitions not generated by OpenAPI.

#### `constants/` — Application Constants

```
constants/
├── api-paths.ts                # API endpoint paths
├── route-paths.ts              # Frontend route paths
├── permissions.ts              # Permission codes
├── status-codes.ts             # Business status codes
├── storage-keys.ts             # Local storage keys
├── query-keys.ts               # TanStack Query keys
└── index.ts
```

**Responsibility:** Magic strings and numbers as named constants.

#### `schemas/` — Zod Schemas

```
schemas/
├── auth.schema.ts              # Login, register schemas
├── order.schema.ts             # Order form schemas
├── product.schema.ts           # Product form schemas
├── common.schema.ts            # Shared field schemas
└── index.ts
```

**Responsibility:** Validation schemas shared across forms. Derived from OpenAPI types.

#### `providers/` — React Context Providers

```
providers/
├── query-provider.tsx          # TanStack Query provider
├── theme-provider.tsx          # Ant Design theme provider
├── auth-provider.tsx           # Auth context provider
├── tenant-provider.tsx         # Tenant context provider
├── websocket-provider.tsx      # WebSocket provider
├── i18n-provider.tsx           # Internationalization provider
└── app-provider.tsx            # Composed providers
```

**Responsibility:** React context setup. Composed in root layout.

#### `layouts/` — Layout Components

```
layouts/
├── admin-layout.tsx            # Main admin layout
├── auth-layout.tsx             # Login/register layout
├── fullscreen-layout.tsx       # Full-screen pages
└── settings-layout.tsx         # Settings page layout
```

**Responsibility:** Page layout shells with sidebar, header, content area.

#### `messages/` — i18n Translation Files

```
messages/
├── zh-CN/
│   ├── common.json
│   ├── auth.json
│   ├── dashboard.json
│   ├── product.json
│   ├── order.json
│   ├── inventory.json
│   └── ...
├── en/
│   ├── common.json
│   ├── auth.json
│   └── ...
```

**Responsibility:** Translation key-value pairs per namespace per locale.

#### `public/` — Static Assets

```
public/
├── images/
│   ├── logo.svg
│   ├── favicon.ico
│   └── placeholders/
├── fonts/
└── documents/
```

**Responsibility:** Static files served directly. No build processing.

---

## 3. Layered Architecture

### 3.1 Layer Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Page Layer                                │
│  app/(dashboard)/order/page.tsx                                  │
│  Responsibility: Route entry, Suspense boundaries, metadata     │
│  Dependencies: Feature Layer only                               │
├─────────────────────────────────────────────────────────────────┤
│                       Feature Layer                              │
│  features/order/                                                 │
│  Responsibility: Business logic orchestration, data flow        │
│  Dependencies: Application Layer, Shared Components             │
├─────────────────────────────────────────────────────────────────┤
│                     Application Layer                            │
│  hooks/, stores/, services/, schemas/, types/                    │
│  Responsibility: Cross-cutting concerns, data fetching, state   │
│  Dependencies: API Layer, Generated Layer                       │
├─────────────────────────────────────────────────────────────────┤
│                         API Layer                                │
│  services/, lib/axios.ts                                        │
│  Responsibility: HTTP communication, error handling, transforms │
│  Dependencies: Generated OpenAPI Client                         │
├─────────────────────────────────────────────────────────────────┤
│                   Generated OpenAPI Client                       │
│  generated/                                                      │
│  Responsibility: Type-safe API client, request/response types   │
│  Dependencies: OpenAPI spec from backend                        │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Layer Responsibilities

| Layer | Responsibility | Example |
|-------|---------------|---------|
| **Page** | Route entry, layout composition, Suspense boundaries, SEO metadata | `app/(dashboard)/order/page.tsx` |
| **Feature** | Business logic orchestration, component composition, data flow coordination | `features/order/components/order-list.tsx` |
| **Application** | Hooks (data fetching, state), stores (client state), schemas (validation) | `hooks/use-auth.ts`, `stores/auth-store.ts` |
| **API** | HTTP client setup, interceptors, error mapping, request transformation | `services/order.service.ts` |
| **Generated** | Type-safe API clients, TypeScript interfaces from OpenAPI | `generated/api/order-api.ts` |

### 3.3 Dependency Rules

```
Page ──→ Feature ──→ Application ──→ API ──→ Generated
  │         │            │
  │         │            └──→ Shared Components
  │         └──→ Shared Components
  └──→ Layouts
```

**Strict rules:**
1. **No upward dependencies** — Lower layers never import from higher layers
2. **No cross-feature imports** — `features/order` never imports from `features/product` directly; use shared hooks or events
3. **No direct API calls in pages** — Always go through feature hooks
4. **Generated code is read-only** — Never modify generated files; extend via service wrappers

### 3.4 Data Flow Diagram

```
User Action
    │
    ▼
Page Component (RSC or Client)
    │
    ▼
Feature Hook (useQuery / useMutation)
    │
    ├──→ TanStack Query Cache ──→ (stale? refetch)
    │
    ▼
Service Function
    │
    ▼
Axios Instance + Interceptors
    │
    ▼
Generated API Client
    │
    ▼
HTTP Request → Gateway → Microservice
    │
    ▼
Response → Error Handler → Transform → Cache Update → UI Re-render
```

---

## 4. Feature-Based Module Structure

### 4.1 Module Template

Each feature module follows this structure:

```
features/{module}/
├── components/                 # Module-specific components
│   ├── {module}-list.tsx       # List/table component
│   ├── {module}-form.tsx       # Create/edit form
│   ├── {module}-detail.tsx     # Detail view
│   ├── {module}-columns.tsx    # Table column definitions
│   └── index.ts
├── hooks/                      # Module-specific hooks
│   ├── use-{module}-list.ts    # List query hook
│   ├── use-{module}-detail.ts  # Detail query hook
│   ├── use-{module}-mutation.ts # Create/update/delete mutations
│   └── index.ts
├── services/                   # Module API services
│   ├── {module}.service.ts     # API calls
│   └── index.ts
├── schemas/                    # Module Zod schemas
│   ├── {module}.schema.ts      # Form validation schemas
│   └── index.ts
├── types/                      # Module TypeScript types
│   ├── {module}.types.ts       # Module-specific interfaces
│   └── index.ts
├── constants/                  # Module constants
│   ├── {module}.constants.ts   # Status codes, labels, etc.
│   └── index.ts
├── utils/                      # Module utilities
│   ├── {module}.utils.ts       # Helper functions
│   └── index.ts
└── index.ts                    # Public API barrel export
```

### 4.2 Module Details

#### 4.2.1 Dashboard Module

```
features/dashboard/
├── components/
│   ├── kpi-cards.tsx                   # Key performance indicators
│   ├── sales-chart.tsx                 # Sales trend chart
│   ├── order-status-chart.tsx          # Order status distribution
│   ├── inventory-alerts.tsx            # Low stock alerts
│   ├── recent-orders.tsx               # Recent orders table
│   ├── top-products.tsx                # Top selling products
│   ├── revenue-chart.tsx               # Revenue trend
│   ├── widget-grid.tsx                 # Drag-and-drop widget grid
│   ├── widget-card.tsx                 # Individual widget wrapper
│   └── quick-actions.tsx               # Quick action buttons
├── hooks/
│   ├── use-dashboard-stats.ts          # Dashboard statistics
│   ├── use-kpi-data.ts                 # KPI metrics
│   ├── use-widget-layout.ts            # Widget layout persistence
│   └── index.ts
├── services/
│   ├── dashboard.service.ts
│   └── index.ts
├── types/
│   ├── dashboard.types.ts              # KPI, Widget, ChartData types
│   └── index.ts
└── index.ts
```

#### 4.2.2 Product Module

```
features/product/
├── components/
│   ├── product-list.tsx                # Product list with ProTable
│   ├── product-form.tsx                # Create/edit product form
│   ├── product-detail.tsx              # Product detail view
│   ├── product-columns.tsx             # Table column definitions
│   ├── spu-form.tsx                    # SPU form
│   ├── sku-form.tsx                    # SKU form (nested)
│   ├── category-tree.tsx               # Category tree selector
│   ├── brand-selector.tsx              # Brand dropdown
│   ├── attribute-template-form.tsx     # Attribute template editor
│   ├── product-search.tsx              # Elasticsearch search
│   ├── product-gallery.tsx             # Image gallery
│   └── index.ts
├── hooks/
│   ├── use-product-list.ts
│   ├── use-product-detail.ts
│   ├── use-product-mutation.ts
│   ├── use-category-tree.ts
│   ├── use-brand-list.ts
│   ├── use-attribute-template.ts
│   └── index.ts
├── services/
│   ├── product.service.ts
│   ├── category.service.ts
│   ├── brand.service.ts
│   └── index.ts
├── schemas/
│   ├── product.schema.ts
│   ├── spu.schema.ts
│   ├── sku.schema.ts
│   └── index.ts
├── types/
│   ├── product.types.ts
│   └── index.ts
├── constants/
│   ├── product.constants.ts
│   └── index.ts
└── index.ts
```

#### 4.2.3 Inventory Module

```
features/inventory/
├── components/
│   ├── inventory-list.tsx
│   ├── inventory-detail.tsx
│   ├── stock-alert-panel.tsx           # Low stock alerts
│   ├── reservation-list.tsx            # Stock reservations
│   ├── snapshot-chart.tsx              # Inventory snapshots over time
│   ├── stock-adjustment-form.tsx       # Manual stock adjustment
│   ├── movement-history.tsx            # Stock movement log
│   └── index.ts
├── hooks/
│   ├── use-inventory-list.ts
│   ├── use-inventory-detail.ts
│   ├── use-stock-alerts.ts
│   ├── use-reservation-list.ts
│   ├── use-snapshot-data.ts
│   └── index.ts
├── services/
│   ├── inventory.service.ts
│   └── index.ts
├── types/
│   ├── inventory.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.4 Order Module

```
features/order/
├── components/
│   ├── order-list.tsx
│   ├── order-detail.tsx
│   ├── order-form.tsx                  # Create order
│   ├── order-columns.tsx
│   ├── order-status-flow.tsx           # State machine visualization
│   ├── order-timeline.tsx              # Status history timeline
│   ├── order-items-table.tsx           # Order items sub-table
│   ├── payment-info.tsx                # Payment details
│   ├── refund-form.tsx                 # Refund request
│   └── index.ts
├── hooks/
│   ├── use-order-list.ts
│   ├── use-order-detail.ts
│   ├── use-order-mutation.ts
│   ├── use-order-status.ts
│   └── index.ts
├── services/
│   ├── order.service.ts
│   └── index.ts
├── schemas/
│   ├── order.schema.ts
│   └── index.ts
├── types/
│   ├── order.types.ts
│   └── index.ts
├── constants/
│   ├── order.constants.ts              # Order status enum, labels
│   └── index.ts
└── index.ts
```

#### 4.2.5 Warehouse Module

```
features/warehouse/
├── components/
│   ├── warehouse-list.tsx
│   ├── warehouse-form.tsx
│   ├── location-tree.tsx               # Warehouse location hierarchy
│   ├── inbound-list.tsx
│   ├── inbound-form.tsx
│   ├── outbound-list.tsx
│   ├── outbound-form.tsx
│   ├── wave-picking-list.tsx
│   ├── wave-picking-detail.tsx
│   ├── warehouse-map.tsx               # Visual warehouse layout
│   └── index.ts
├── hooks/
│   ├── use-warehouse-list.ts
│   ├── use-location-tree.ts
│   ├── use-inbound-list.ts
│   ├── use-outbound-list.ts
│   ├── use-wave-picking.ts
│   └── index.ts
├── services/
│   ├── warehouse.service.ts
│   └── index.ts
├── types/
│   ├── warehouse.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.6 Purchase Module

```
features/purchase/
├── components/
│   ├── purchase-order-list.tsx
│   ├── purchase-order-form.tsx
│   ├── rfq-list.tsx                    # Request for Quotation
│   ├── rfq-form.tsx
│   ├── quotation-list.tsx
│   ├── quotation-comparison.tsx        # Price comparison table
│   ├── purchase-plan-list.tsx
│   ├── purchase-contract-form.tsx
│   ├── receipt-list.tsx
│   ├── receipt-form.tsx
│   └── index.ts
├── hooks/
│   ├── use-purchase-order.ts
│   ├── use-rfq.ts
│   ├── use-quotation.ts
│   ├── use-purchase-plan.ts
│   └── index.ts
├── services/
│   ├── purchase.service.ts
│   └── index.ts
├── types/
│   ├── purchase.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.7 Supplier Module

```
features/supplier/
├── components/
│   ├── supplier-list.tsx
│   ├── supplier-form.tsx
│   ├── supplier-detail.tsx
│   ├── supplier-evaluation.tsx         # Evaluation scores
│   ├── settlement-list.tsx
│   ├── settlement-form.tsx
│   ├── supplier-comparison.tsx         # Supplier comparison chart
│   └── index.ts
├── hooks/
│   ├── use-supplier-list.ts
│   ├── use-supplier-detail.ts
│   ├── use-supplier-evaluation.ts
│   ├── use-settlement.ts
│   └── index.ts
├── services/
│   ├── supplier.service.ts
│   └── index.ts
├── types/
│   ├── supplier.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.8 Logistics Module

```
features/logistics/
├── components/
│   ├── waybill-list.tsx
│   ├── waybill-detail.tsx
│   ├── tracking-timeline.tsx           # Shipment tracking timeline
│   ├── carrier-list.tsx
│   ├── route-list.tsx
│   ├── route-map.tsx                   # Route visualization
│   ├── delivery-area-map.tsx           # Delivery zone map
│   ├── tracking-map.tsx                # Real-time tracking map
│   └── index.ts
├── hooks/
│   ├── use-waybill.ts
│   ├── use-tracking.ts
│   ├── use-carrier.ts
│   ├── use-route.ts
│   └── index.ts
├── services/
│   ├── logistics.service.ts
│   └── index.ts
├── types/
│   ├── logistics.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.9 Finance Module

```
features/finance/
├── components/
│   ├── settlement-list.tsx
│   ├── settlement-detail.tsx
│   ├── invoice-list.tsx
│   ├── invoice-form.tsx
│   ├── reconciliation-list.tsx
│   ├── reconciliation-detail.tsx
│   ├── freight-rule-list.tsx
│   ├── freight-rule-form.tsx
│   ├── platform-fee-list.tsx
│   ├── finance-dashboard.tsx           # Financial overview
│   └── index.ts
├── hooks/
│   ├── use-settlement.ts
│   ├── use-invoice.ts
│   ├── use-reconciliation.ts
│   ├── use-freight-rule.ts
│   └── index.ts
├── services/
│   ├── finance.service.ts
│   └── index.ts
├── types/
│   ├── finance.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.10 Tenant Module

```
features/tenant/
├── components/
│   ├── tenant-list.tsx
│   ├── tenant-form.tsx
│   ├── tenant-detail.tsx
│   ├── tenant-config-form.tsx
│   ├── tenant-package-list.tsx
│   ├── tenant-feature-toggle.tsx       # Feature flag management
│   ├── tenant-resource-quota.tsx       # Resource quota display
│   ├── tenant-operation-log.tsx        # Tenant operation history
│   └── index.ts
├── hooks/
│   ├── use-tenant-list.ts
│   ├── use-tenant-config.ts
│   ├── use-tenant-package.ts
│   └── index.ts
├── services/
│   ├── tenant.service.ts
│   └── index.ts
├── types/
│   ├── tenant.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.11 System Module

```
features/system/
├── components/
│   ├── user/
│   │   ├── user-list.tsx
│   │   ├── user-form.tsx
│   │   ├── user-detail.tsx
│   │   ├── user-role-assignment.tsx
│   │   └── index.ts
│   ├── role/
│   │   ├── role-list.tsx
│   │   ├── role-form.tsx
│   │   ├── role-permission-tree.tsx    # Permission assignment tree
│   │   └── index.ts
│   ├── permission/
│   │   ├── permission-list.tsx
│   │   ├── permission-form.tsx
│   │   ├── permission-tree.tsx
│   │   └── index.ts
│   ├── dept/
│   │   ├── dept-tree.tsx
│   │   ├── dept-form.tsx
│   │   └── index.ts
│   └── index.ts
├── hooks/
│   ├── use-user.ts
│   ├── use-role.ts
│   ├── use-permission.ts
│   ├── use-dept.ts
│   └── index.ts
├── services/
│   ├── user.service.ts
│   ├── role.service.ts
│   ├── permission.service.ts
│   └── index.ts
├── types/
│   ├── system.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.12 Dictionary Module

```
features/dictionary/
├── components/
│   ├── dict-list.tsx
│   ├── dict-form.tsx
│   ├── dict-item-list.tsx
│   ├── dict-item-form.tsx
│   ├── dict-selector.tsx               # Reusable dict dropdown
│   ├── status-dict-selector.tsx        # Status-specific selector
│   └── index.ts
├── hooks/
│   ├── use-dict.ts
│   ├── use-dict-item.ts
│   └── index.ts
├── services/
│   ├── dict.service.ts
│   └── index.ts
├── types/
│   ├── dict.types.ts
│   └── index.ts
└── index.ts
```

#### 4.2.13 Notification Module

```
features/notification/
├── components/
│   ├── notification-list.tsx
│   ├── notification-detail.tsx
│   ├── notification-bell.tsx           # Header bell icon with badge
│   ├── notification-dropdown.tsx       # Quick notification dropdown
│   ├── notification-template-form.tsx  # Template editor
│   ├── notification-audit.tsx          # Audit log
│   ├── preference-form.tsx             # User notification preferences
│   └── index.ts
├── hooks/
│   ├── use-notification.ts
│   ├── use-notification-count.ts
│   ├── use-notification-template.ts
│   └── index.ts
├── services/
│   ├── notification.service.ts
│   └── index.ts
├── types/
│   ├── notification.types.ts
│   └── index.ts
└── index.ts
```

---

## 5. Authentication Architecture

### 5.1 JWT Token Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                    Token Lifecycle                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Login ──→ Access Token (15min) ──→ API Calls                   │
│    │              │                                              │
│    │              ▼                                              │
│    │         Expired? ──→ Refresh Token (7d) ──→ New Access     │
│    │              │              │                               │
│    │              │              ▼                               │
│    │              │         Refresh Expired? ──→ Login Page      │
│    │              │                                              │
│    ▼              ▼                                              │
│  Remember Me ──→ Extended Refresh (30d)                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Token Storage:**
- Access Token: Memory (Zustand store, not localStorage)
- Refresh Token: HttpOnly cookie (set by backend) or encrypted localStorage
- User Info: Zustand store + sessionStorage backup

**ADR-006:** Access tokens stored in memory only (Zustand) to prevent XSS theft. Refresh tokens in HttpOnly cookies to prevent JavaScript access. Fallback: encrypted localStorage if cookies are not feasible in development.

### 5.2 Axios Interceptor Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                   Request Interceptor                            │
├─────────────────────────────────────────────────────────────────┤
│  1. Read access token from authStore                            │
│  2. Set Authorization: Bearer {token}                           │
│  3. Set X-Tenant-Id header from tenantStore                     │
│  4. Set X-Request-Id (UUID v7) for tracing                     │
│  5. Set Accept-Language from current locale                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   Response Interceptor                           │
├─────────────────────────────────────────────────────────────────┤
│  1. Check response status                                       │
│  2. If 401 → attempt token refresh                              │
│     a. Queue pending requests                                   │
│     b. Call /api/auth/refresh                                   │
│     c. Update authStore with new token                          │
│     d. Retry queued requests                                    │
│     e. If refresh fails → redirect to /login                    │
│  3. If 403 → show permission denied message                     │
│  4. If 404 → redirect to /not-found                             │
│  5. If 422 → return validation errors for form binding          │
│  6. If 429 → show rate limit message                            │
│  7. If 500 → show generic error toast                           │
│  8. If success → unwrap response data                           │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3 401 Retry Mechanism

The token refresh queue ensures concurrent requests during token refresh are properly handled:

1. First 401 triggers refresh, subsequent 401s queue
2. All queued requests wait for refresh result
3. On success: retry all queued requests with new token
4. On failure: redirect all to login

### 5.4 Route Guards (Next.js Middleware)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Middleware Flow                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Request ──→ Middleware                                         │
│    │                                                             │
│    ├── Public route? (/login, /register) ──→ Allow              │
│    │                                                             │
│    ├── No token? ──→ Redirect to /login                        │
│    │                                                             │
│    ├── Token expired? ──→ Try refresh ──→ Success? ──→ Allow   │
│    │                                            └──→ /login     │
│    │                                                             │
│    ├── Has permission? ──→ Allow                                │
│    │         └── No ──→ /403                                    │
│    │                                                             │
│    └── Allow + set tenant header                                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.5 Multi-Tenant Header

Every API request includes:
- `X-Tenant-Id`: Current tenant ID (from tenantStore)
- `X-Tenant-Code`: Current tenant code (for debugging)

Tenant switching:
1. User selects tenant from `TenantSwitcher` component
2. `tenantStore.setTenant(tenantId)` updates Zustand store
3. Axios interceptor reads from store on next request
4. Backend routes to correct database via `@DS` annotation

### 5.6 RBAC Permission System

```
┌─────────────────────────────────────────────────────────────────┐
│                    Permission Model                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User ──→ Roles ──→ Permissions                                │
│                    │                                             │
│                    ├── Menu permissions (route access)           │
│                    ├── Button permissions (action access)        │
│                    └── Data permissions (scope: own/dept/all)    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Permission Cache:**
- Store permissions in Zustand after login
- Cache TTL: Match session lifetime
- Invalidate on: role change, permission update, logout

### 5.7 Logout Flow

```
User clicks Logout
    │
    ├── Call POST /api/auth/logout
    │
    ├── Clear authStore (tokens, user info)
    ├── Clear tenantStore
    ├── Clear notificationStore
    ├── Clear TanStack Query cache
    ├── Clear sessionStorage
    ├── Disconnect WebSocket
    │
    └── Redirect to /login
```

### 5.8 Authentication Sequence Diagram

```
┌──────┐          ┌─────────┐        ┌─────────┐       ┌──────────┐
│Browser│          │ Next.js │        │ Gateway │       │Auth Svc  │
└──┬───┘          └────┬────┘        └────┬────┘       └────┬─────┘
   │                   │                   │                 │
   │ POST /login       │                   │                 │
   │──────────────────→│                   │                 │
   │                   │ POST /api/auth/login                │
   │                   │──────────────────→│────────────────→│
   │                   │                   │                 │
   │                   │                   │  LoginResponse  │
   │                   │←──────────────────│←────────────────│
   │                   │                   │                 │
   │                   │ Set cookies       │                 │
   │                   │ Store tokens      │                 │
   │  200 + Set-Cookie │                   │                 │
   │←──────────────────│                   │                 │
   │                   │                   │                 │
   │ GET /dashboard    │                   │                 │
   │──────────────────→│                   │                 │
   │                   │ RSC fetch         │                 │
   │                   │ Authorization: Bearer {token}       │
   │                   │──────────────────→│────────────────→│
   │                   │                   │                 │
   │                   │                   │  200 + data     │
   │                   │←──────────────────│←────────────────│
   │                   │                   │                 │
   │  Rendered page    │                   │                 │
   │←──────────────────│                   │                 │
```

---

## 6. Routing Architecture

### 6.1 App Router Structure

```
app/
├── layout.tsx                          # Root layout (html, body, providers)
├── globals.css                         # Global styles
├── not-found.tsx                       # Global 404
├── error.tsx                           # Global error boundary
│
├── [locale]/                           # Locale segment (zh-CN, en)
│   ├── layout.tsx                      # Locale layout (i18n provider)
│   │
│   ├── (auth)/                         # Auth route group (no sidebar)
│   │   ├── layout.tsx                  # Auth layout (centered card)
│   │   ├── login/page.tsx
│   │   ├── register/page.tsx
│   │   └── forgot-password/page.tsx
│   │
│   └── (app)/                          # App route group (with sidebar)
│       ├── layout.tsx                  # Admin layout (ProLayout)
│       ├── loading.tsx                 # App loading skeleton
│       │
│       ├── dashboard/page.tsx          # /zh-CN/dashboard
│       │
│       ├── product/
│       │   ├── page.tsx                # /zh-CN/product (list)
│       │   ├── create/page.tsx         # /zh-CN/product/create
│       │   └── [id]/
│       │       ├── page.tsx            # /zh-CN/product/123 (detail)
│       │       └── edit/page.tsx       # /zh-CN/product/123/edit
│       │
│       ├── inventory/
│       │   ├── page.tsx
│       │   ├── [id]/page.tsx
│       │   └── alerts/page.tsx
│       │
│       ├── order/
│       │   ├── page.tsx
│       │   ├── create/page.tsx
│       │   ├── [id]/
│       │   │   ├── page.tsx
│       │   │   └── track/page.tsx
│       │   └── refund/[id]/page.tsx
│       │
│       ├── warehouse/
│       │   ├── page.tsx
│       │   ├── [id]/page.tsx
│       │   ├── inbound/page.tsx
│       │   ├── outbound/page.tsx
│       │   └── wave-picking/page.tsx
│       │
│       ├── purchase/
│       │   ├── page.tsx
│       │   ├── create/page.tsx
│       │   ├── [id]/page.tsx
│       │   ├── rfq/page.tsx
│       │   └── quotation/page.tsx
│       │
│       ├── supplier/
│       │   ├── page.tsx
│       │   ├── [id]/page.tsx
│       │   └── settlement/page.tsx
│       │
│       ├── logistics/
│       │   ├── page.tsx
│       │   ├── waybill/[id]/page.tsx
│       │   ├── tracking/page.tsx
│       │   └── carrier/page.tsx
│       │
│       ├── finance/
│       │   ├── page.tsx
│       │   ├── settlement/page.tsx
│       │   ├── invoice/page.tsx
│       │   └── reconciliation/page.tsx
│       │
│       ├── tenant/
│       │   ├── page.tsx
│       │   ├── [id]/page.tsx
│       │   ├── package/page.tsx
│       │   └── config/page.tsx
│       │
│       ├── system/
│       │   ├── user/page.tsx
│       │   ├── role/page.tsx
│       │   ├── permission/page.tsx
│       │   ├── dept/page.tsx
│       │   └── dictionary/page.tsx
│       │
│       ├── notification/
│       │   ├── page.tsx
│       │   └── [id]/page.tsx
│       │
│       └── settings/
│           ├── profile/page.tsx
│           ├── security/page.tsx
│           └── preferences/page.tsx
│
├── (marketing)/                        # Public marketing pages
│   ├── page.tsx                        # Landing page
│   └── about/page.tsx
│
└── api/                                # API routes (BFF)
    └── auth/
        └── [...nextauth]/route.ts
```

### 6.2 Route Groups

| Group | Layout | Auth | Sidebar | Use Case |
|-------|--------|------|---------|----------|
| `(auth)` | Centered card | No | No | Login, register, forgot password |
| `(app)` | Admin shell (ProLayout) | Yes | Yes | All business pages |
| `(marketing)` | Marketing layout | No | No | Landing, about |
| `[locale]` | Locale wrapper | - | - | i18n locale prefix |

### 6.3 Route Metadata

Route configuration defines title (i18n key), icon, required permission code, breadcrumb visibility, and parent route for breadcrumb chain.

### 6.4 Breadcrumbs

Auto-generated from route config + parent chain:
```
Dashboard > Product > Product Detail > Edit
```

### 6.5 Loading & Error Pages

Each major section has a `loading.tsx` with a ProSkeleton matching the page layout, and an `error.tsx` for error boundaries.

### 6.6 Suspense Boundaries

Each page wraps its main content in `<Suspense>` with appropriate fallbacks.

### 6.7 Parallel Routes (Dashboard)

Dashboard uses parallel routes (`@stats`, `@charts`, `@alerts`) for concurrent data loading of independent panels.

---

## 7. API Layer Design

### 7.1 Axios Instance Configuration

- **Base URL:** `http://localhost:8761` (Gateway)
- **Default timeout:** 30s
- **Content-Type:** `application/json`

### 7.2 Error Handling Strategy

| HTTP Code | Handling |
|-----------|----------|
| 400 | Bad Request → Show validation errors on form |
| 401 | Unauthorized → Refresh token → Retry → Login page |
| 403 | Forbidden → Show "No permission" modal |
| 404 | Not Found → Redirect to /not-found |
| 408 | Timeout → Auto-retry (1x) → Show timeout message |
| 409 | Conflict → Show conflict message, suggest refresh |
| 422 | Validation → Bind errors to form fields |
| 429 | Rate Limit → Show "Too many requests" + cooldown |
| 500 | Server Error → Show error toast, log to Sentry |
| 502/503 | Gateway Error → Show "Service unavailable" + retry |

### 7.3 Response Wrapper

Backend returns `ApiResponse<T>` with `code`, `message`, `data`, `traceId`, `timestamp`. Interceptor unwraps to return `data` directly on success.

### 7.4 Retry Strategy

- Max 2 retries for network errors and retryable server errors
- Exponential backoff: 1s, 2s, 4s (max 10s)
- No retry for mutations (POST/PUT/DELETE)

### 7.5 Timeout Strategy

| API Type | Timeout | Rationale |
|----------|---------|-----------|
| Normal CRUD | 30s | Standard operations |
| File upload | 5min | Large file uploads |
| File download | 5min | Large file downloads |
| Report generation | 2min | Complex queries |
| Search | 10s | Elasticsearch queries |
| Auth endpoints | 15s | Login/register |

### 7.6 Pagination

Standard `PageRequest` with `pageNum` (1-based), `pageSize`, `orderBy`, `orderDir`. Response includes `records`, `total`, `pageNum`, `pageSize`, `pages`.

### 7.7 Upload API

Chunk upload flow: calculate hash → check deduplication → split into 5MB chunks → upload 4 chunks in parallel → merge on server → return URL.

### 7.8 Download API

File download with progress tracking using `responseType: 'blob'` and `onDownloadProgress`.

### 7.9 OpenAPI Generated Clients

Backend exposes OpenAPI spec at `/v3/api-docs`. `npm run generate:api` generates TypeScript types + Axios clients. Service wrappers add business logic on top. Never edit generated files directly.

---

## 8. TanStack Query Architecture

### 8.1 QueryClient Configuration

- **staleTime:** 5 minutes (data considered fresh)
- **gcTime:** 30 minutes (garbage collection)
- **retry:** 2 (retry failed queries)
- **refetchOnWindowFocus:** false (admin apps don't need this)
- **refetchOnReconnect:** true

### 8.2 Query Keys Convention

Hierarchical key structure:
```
products.all → products.lists() → products.list(filters)
products.all → products.details() → products.detail(id)
```

### 8.3 Invalidation Strategy

- After create/update/delete: invalidate related list queries
- After status change: invalidate both detail and list queries
- After cross-module operations: invalidate dashboard stats

### 8.4 Optimistic Updates

1. Cancel outgoing refetches
2. Snapshot previous value
3. Optimistically update cache
4. On error: rollback to snapshot
5. On settled: refetch for consistency

### 8.5 Infinite Query

Use `useInfiniteQuery` for paginated lists with "Load More" or infinite scroll.

### 8.6 Prefetching

- Prefetch on hover (product rows, order rows)
- Prefetch next page when current page loads

### 8.7 Cache Strategy by Data Type

| Data Type | staleTime | gcTime | refetchOnWindowFocus |
|-----------|-----------|--------|---------------------|
| Dictionary data | 30min | 2hr | No |
| User permissions | Session | Session | No |
| Product list | 5min | 30min | No |
| Order list | 2min | 15min | No |
| Inventory | 30s | 5min | Yes |
| Dashboard stats | 1min | 10min | Yes |

---

## 9. State Management

### 9.1 State Classification

| State Type | Storage | Examples |
|------------|---------|----------|
| Server state | TanStack Query | API data, paginated lists, details |
| Client state | Zustand | Auth tokens, UI state, tenant, notifications |
| URL state | Next.js router | Current page, query parameters, locale |
| Form state | React Hook Form | Form values, validation, dirty/touched |
| Local UI state | useState | Modal open/close, dropdown visibility |

### 9.2 Zustand Stores

#### AuthStore
- `accessToken`, `refreshToken`, `user`, `permissions`, `isAuthenticated`
- Actions: `setTokens`, `setUser`, `setPermissions`, `clearAuth`, `hasPermission`
- Persisted: No (memory only for security)

#### UIStore
- `sidebarCollapsed`, `themeMode`, `density`, `tabNavigation`, `breadcrumb`
- Actions: `toggleSidebar`, `setThemeMode`, `setDensity`
- Persisted: Yes (localStorage)

#### TenantStore
- `currentTenant`, `tenantList`, `tenantLoading`
- Actions: `setTenant`, `setTenantList`, `clearTenant`
- Persisted: Yes (localStorage, tenant ID only)

#### PreferenceStore
- `locale`, `tablePageSize`, `dateFormat`, `notifications`
- Actions: `setLocale`, `setTablePageSize`, `updateNotifications`
- Persisted: Yes (localStorage)

#### NotificationStore
- `notifications`, `unreadCount`, `connected`
- Actions: `addNotification`, `markAsRead`, `markAllAsRead`, `setConnected`
- Persisted: No

---

## 10. Form Architecture

### 10.1 React Hook Form + Zod Integration

Zod schemas define validation rules. `@hookform/resolvers` bridges Zod to React Hook Form. Types inferred from Zod schemas ensure end-to-end type safety.

### 10.2 Form Hook Pattern

Custom hooks encapsulate form logic: schema definition, default values, query for edit data, population effect, create/update mutations, submit handler.

### 10.3 ProForm Integration

Use Ant Design ProForm with React Hook Form via `FormProvider`. ProForm provides layout, while React Hook Form manages state.

### 10.4 Wizard Forms

Multi-step forms with step validation. Each step validates its fields before allowing progression.

### 10.5 Modal/Drawer Forms

Reusable `FormModal` and `FormDrawer` components that manage open/close state and reset form on close.

---

## 11. Shared Components System

### 11.1 Component Categories

| Category | Purpose | Examples |
|----------|---------|----------|
| `ui/` | Primitive UI elements | Button, Input, Select, Modal |
| `business/` | Business-specific shared | TenantSwitcher, StatusTag, AuditTimeline |
| `table/` | Table utilities | ProTableWrapper, ExportButton, BatchActions |
| `form/` | Form utilities | FormModal, FormDrawer, WizardForm |
| `upload/` | File upload | FileUpload, ImageUpload, ChunkUpload |
| `charts/` | Chart wrappers | LineChart, BarChart, PieChart |
| `editor/` | Code/content editors | MonacoEditor, MarkdownEditor |
| `flow/` | Flow diagrams | WorkflowDesigner, ApprovalFlowViewer |
| `layout/` | Layout components | AdminShell, Sidebar, Header |

### 11.2 Component Design Principles

1. **Single responsibility** — Each component does one thing well
2. **Composition over inheritance** — Use children, render props, or slots
3. **Type safety** — All props fully typed with TypeScript interfaces
4. **Accessibility** — ARIA labels, keyboard navigation, focus management
5. **Responsive** — Mobile-first, breakpoint-aware
6. **Themeable** — Use Ant Design tokens, not hardcoded values

---

## 12. Table Architecture

### 12.1 ProTable Standards

Standard ProTable configuration includes:
- Search area with auto-generated form
- Pagination with backend integration
- Toolbar with action buttons (Create, Export, Import)
- Column settings with localStorage persistence
- Row selection for batch operations
- Row actions (Edit, View, Delete)
- Sorting, filtering, column resize
- Sticky header with vertical scroll

### 12.2 Table Features

| Feature | Implementation |
|---------|---------------|
| Search area | ProTable `search` prop |
| Pagination | ProTable `pagination` prop |
| Toolbar | ProTable `toolbar` prop |
| Column settings | `columnsState` with localStorage |
| Export | Custom `ExportButton` |
| Import | Custom `ImportButton` with Excel |
| Batch operations | `rowSelection` + batch actions |
| Permissions | Conditional rendering via `usePermission()` |
| Virtual scrolling | `virtual` prop for 1000+ rows |

### 12.3 Reusable Table Hook

`useProTable` hook manages pagination, filters, sorter state and integrates with TanStack Query.

---

## 13. Theme System

### 13.1 Ant Design Theme Configuration

Light theme: white containers, blue primary, standard spacing.
Dark theme: dark containers, same primary, adjusted contrast.

### 13.2 CSS Variables

Define CSS variables for primary, success, warning, error colors, background colors, text colors, sidebar width, header height. Support `[data-theme='dark']` selector.

### 13.3 Responsive Breakpoints

| Breakpoint | Width | Device |
|------------|-------|--------|
| xs | 480px | Mobile |
| sm | 576px | Large mobile |
| md | 768px | Tablet |
| lg | 992px | Small desktop |
| xl | 1200px | Desktop |
| xxl | 1600px | Large desktop |

### 13.4 Density Modes

| Mode | Table size | Font size | Use case |
|------|-----------|-----------|----------|
| Default | Middle | 14px | Standard view |
| Compact | Small | 13px | Data-dense pages |
| Loose | Large | 15px | Presentation mode |

---

## 14. Internationalization

### 14.1 next-intl Configuration

Use `getRequestConfig` for server-side locale loading. Messages loaded from `messages/{locale}.json`.

### 14.2 Locale Routing

```
/zh-CN/dashboard          # Chinese
/en/dashboard             # English
/dashboard                # Default (zh-CN)
```

Middleware handles locale detection: URL prefix → Accept-Language header → cookie → default.

### 14.3 Namespace Strategy

- `common.json` — Shared UI labels
- `auth.json` — Login, register pages
- `dashboard.json` — Dashboard page
- `{module}.json` — Per-module translations
- `validation.json` — Form validation messages

### 14.4 Server/Client Component Support

- Server Components: Translations loaded on server (zero client JS)
- Client Components: Translations bundled with client code
- Lazy loading: Load module translations on demand

---

## 15. Real-time Architecture

### 15.1 STOMP WebSocket Connection

Use `@stomp/stompjs` with:
- Broker URL: `ws://localhost:8761/ws`
- Auth header: Bearer token
- Tenant header: X-Tenant-Id
- Heartbeat: 10s incoming/outgoing
- Reconnect delay: 5s

### 15.2 Reconnect Strategy

Exponential backoff: 5s → 10s → 20s → 40s → 60s. Max 5 attempts. Show offline UI after all attempts fail. Immediate reconnect on page visibility change.

### 15.3 Subscription Channels

| Channel | Type | Use Case |
|---------|------|----------|
| `/user/queue/notifications` | User-specific | Personal notifications |
| `/topic/approvals` | Broadcast | Approval workflow updates |
| `/user/queue/inventory-alerts` | User-specific | Low stock alerts |
| `/topic/system-announcements` | Broadcast | System-wide announcements |

### 15.4 Event Bus Pattern

Simple pub/sub event bus for cross-component communication without prop drilling.

### 15.5 Real-time Sequence Diagram

```
Browser ──WS Connect──→ Next.js ──STOMP──→ RabbitMQ
Browser ──Subscribe──→ Next.js ──Subscribe──→ RabbitMQ
OrderSvc ──Publish──→ RabbitMQ ──Notification──→ Next.js ──STOMP Frame──→ Browser
Browser ──Update UI──→ Toast + Badge
```

---

## 16. Dashboard Architecture

### 16.1 Dashboard Layout

4 KPI cards at top, 2 charts in middle, 2 data panels (recent orders + top products) below, inventory alerts at bottom.

### 16.2 Widget System

Configurable widget grid with types: kpi, chart, table, list, alert. Each widget has configurable span (grid columns) and refresh interval.

### 16.3 Refresh Strategy

| Widget | Auto-refresh | Cache TTL |
|--------|-------------|-----------|
| KPI cards | 1 min | 30s |
| Charts | 5 min | 2min |
| Recent orders | 2 min | 1min |
| Inventory alerts | 30s | 15s |

### 16.4 ECharts Integration

`ChartContainer` component handles ECharts initialization, theme switching, responsive resize, and option updates.

---

## 17. File Management Architecture

### 17.1 Upload Flow

Select file → Validate (type, size) → Calculate hash (MD5) → Check deduplication → Split into 5MB chunks → Upload 4 chunks in parallel → Merge on server → Return URL.

### 17.2 Component Suite

| Component | Use Case | Features |
|-----------|----------|----------|
| `FileUpload` | Generic file upload | Drag & drop, multiple files, progress |
| `ImageUpload` | Image upload with preview | Crop, compress, gallery |
| `ChunkUpload` | Large file upload | Chunked, resumable, progress |
| `ExcelImport` | Excel data import | Template download, validation, preview |

### 17.3 Preview Support

| File Type | Preview Method |
|-----------|---------------|
| Images | Built-in Image preview |
| PDF | `react-pdf` or iframe |
| Excel | `xlsx` library + table render |
| Code | Monaco Editor (read-only) |
| Markdown | `react-markdown` |

---

## 18. Error Handling Strategy

### 18.1 Error Boundary Hierarchy

```
Root Error Boundary
├── Auth Error Boundary
└── App Error Boundary
    ├── Module Error Boundaries (per route segment)
    └── Component Error Boundaries (granular)
```

### 18.2 Error Boundary Component

Catches React errors, logs to Sentry, displays fallback UI with retry and go-home actions.

### 18.3 Toast/Message Strategy

| Error Type | Display | Duration |
|------------|---------|----------|
| Validation | Inline on form field | Until fixed |
| API error (4xx) | `message.error()` toast | 5s |
| API error (5xx) | `notification.error()` | Auto-close |
| Network error | `message.error()` + offline banner | Until reconnect |
| Permission denied | Modal | User closes |
| Unexpected error | Error boundary fallback | User action |

### 18.4 Global Exception Pages

404, 403, 500, Offline pages with appropriate messaging and navigation options.

---

## 19. Performance Optimization

### 19.1 Code Splitting Strategy

- **Core bundle:** ~200KB gzipped (React, Next.js, Ant Design core, Zustand, TanStack Query)
- **Route chunks:** Per page (~50-80KB each)
- **Shared chunks:** ProTable, ProForm
- **On-demand chunks:** ECharts (~200KB), Monaco Editor (~500KB), ReactFlow (~150KB)

### 19.2 Dynamic Imports

Heavy components (Monaco, ECharts, ReactFlow) loaded with `next/dynamic` and `ssr: false`.

### 19.3 Memoization Strategy

| Pattern | When | Example |
|---------|------|---------|
| `React.memo` | Pure components that re-render often | Table rows, list items |
| `useMemo` | Expensive computations | Filtering, sorting, aggregations |
| `useCallback` | Functions passed as props | Event handlers, callbacks |

### 19.4 Virtual List

For lists with 1000+ items: use `react-window` or `@tanstack/react-virtual`. ProTable built-in `virtual` prop for large datasets.

### 19.5 Image Optimization

Use `next/image` with lazy loading, blur placeholders, responsive sizes.

### 19.6 Bundle Optimization

Tree shaking (named imports only), dead code elimination (feature flags), Brotli + Gzip compression, immutable assets with content hash.

---

## 20. Security Design

### 20.1 XSS Prevention

React auto-escapes JSX output. Never use `dangerouslySetInnerHTML`. Sanitize third-party content with DOMPurify.

### 20.2 CSRF Prevention

Backend uses `SameSite=Strict` cookies. `Authorization` header (not cookie) for API calls.

### 20.3 JWT Security

| Concern | Mitigation |
|---------|-----------|
| Token theft (XSS) | Access token in memory only |
| Token theft (cookie) | HttpOnly, Secure, SameSite cookies |
| Token expiry | Short-lived access tokens (15min) |
| Refresh token theft | Refresh token rotation |

### 20.4 RBAC Permission Controls

Three levels: route level (middleware), page level (component), action level (button).

### 20.5 Content Security Policy

Restrict script-src, style-src, img-src, connect-src to known origins.

### 20.6 Tenant Isolation

Every API request includes `X-Tenant-Id` header. Backend enforces isolation. Tenant switching clears all caches.

---

## 21. Testing Strategy

### 21.1 Test Pyramid

- **Unit tests (70%):** Hooks, utilities, components
- **Integration tests (20%):** Component interactions, API integration
- **E2E tests (10%):** Critical user flows

### 21.2 Coverage Targets

| Layer | Target |
|-------|--------|
| Hooks | 90%+ |
| Utilities | 95%+ |
| Components | 80%+ |
| Services | 85%+ |

### 21.3 Tools

- **Unit/Integration:** Vitest + React Testing Library
- **E2E:** Playwright
- **Coverage:** Vitest coverage (V8)

---

## 22. CI/CD

### 22.1 Pipeline Stages

```
Lint → Test → Build → E2E → Docker
```

### 22.2 GitHub Actions Workflow

1. **Lint:** ESLint + TypeScript type check
2. **Test:** Unit tests + integration tests with coverage
3. **Build:** Next.js build + bundle analysis
4. **E2E:** Playwright tests against built app
5. **Docker:** Build and push image (main branch only)

### 22.3 Deployment Strategy

| Environment | Trigger | Strategy |
|-------------|---------|----------|
| Development | Push to `develop` | Auto-deploy |
| Staging | Push to `main` | Auto-deploy |
| Production | Manual approval | Blue-green |
| Preview | Pull request | Vercel preview |

### 22.4 Rollback

Docker tags with git SHA. `kubectl rollout undo` for quick rollback. Feature flags for disabling features.

---

## 23. Frontend Observability

### 23.1 Sentry Integration

- DSN from environment variable
- 10% transaction sampling
- 1% session replay sampling
- 100% error replay
- Strip Authorization headers from error reports

### 23.2 Performance Metrics

| Metric | Target |
|--------|--------|
| LCP | < 2.5s |
| FID | < 100ms |
| CLS | < 0.1 |
| TTFB | < 800ms |
| Bundle size | < 250KB gzipped |

### 23.3 Logging

Structured logging with Sentry breadcrumbs. Production errors captured with context.

### 23.4 User Action Tracking

Analytics events for key user actions (create, update, delete) with Sentry breadcrumbs.

---

## 24. Development Standards

### 24.1 ESLint

Extend `next/core-web-vitals`, `@typescript-eslint/strict-type-checked`, `import/typescript`, `prettier`.

### 24.2 Prettier

Semicolons, single quotes, 2-space tabs, trailing commas, 100 char line width, LF line endings.

### 24.3 Commit Convention

Conventional Commits: `feat(product): add product creation form`

### 24.4 Branch Strategy

```
main (production)
├── develop (staging)
│   ├── feature/* (features)
│   └── fix/* (bugfixes)
└── release/* (releases)
```

### 24.5 PR Rules

Title follows convention. Description explains what/why/how. All tests pass. No coverage decrease. At least 1 approval. No lint/type errors.

---

## 25. Dependency Management

### 25.1 Core Dependencies

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `react` | ^19.0 | UI library | ~4KB |
| `react-dom` | ^19.0 | DOM renderer | ~130KB |
| `next` | ^15.0 | Framework | ~90KB |

### 25.2 UI Dependencies

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `antd` | ^5.20 | UI components | ~100KB |
| `@ant-design/pro-components` | ^2.8 | Admin components | ~200KB |
| `@ant-design/icons` | ^5.5 | Icon library | ~50KB |

### 25.3 State Management

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `zustand` | ^5.0 | Client state | ~1.1KB |
| `@tanstack/react-query` | ^5.60 | Server state | ~13KB |

### 25.4 Form Dependencies

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `react-hook-form` | ^7.53 | Form management | ~9KB |
| `zod` | ^3.23 | Schema validation | ~14KB |

### 25.5 Chart Dependencies

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `echarts` | ^5.5 | Chart library | ~200KB (dynamic) |
| `echarts-for-react` | ^3.0 | React wrapper | ~5KB |

### 25.6 Network Dependencies

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `axios` | ^1.7 | HTTP client | ~14KB |
| `axios-retry` | ^4.5 | Retry interceptor | ~3KB |
| `@stomp/stompjs` | ^7.0 | STOMP WebSocket | ~15KB |

### 25.7 Utility Dependencies

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `dayjs` | ^1.11 | Date library | ~7KB |
| `lodash-es` | ^4.17 | Utility functions | ~2KB (tree-shaken) |
| `clsx` | ^2.1 | Class name utility | ~0.5KB |
| `ahooks` | ^3.8 | React hooks library | ~10KB |

### 25.8 Internationalization

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `next-intl` | ^3.22 | i18n for Next.js | ~15KB |

### 25.9 Markdown & Editor

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `react-markdown` | ^9.0 | Markdown renderer | ~15KB |
| `@monaco-editor/react` | ^4.6 | Code editor | ~500KB (dynamic) |

### 25.10 Flow & Diagram

| Package | Version | Purpose | Size |
|---------|---------|---------|------|
| `reactflow` | ^11.11 | Flow diagrams | ~150KB (dynamic) |
| `mermaid` | ^11.0 | Diagram rendering | ~200KB (dynamic) |

### 25.11 DevDependencies

| Package | Version | Purpose |
|---------|---------|---------|
| `typescript` | ^5.6 | Type checking |
| `vitest` | ^2.1 | Unit testing |
| `@testing-library/react` | ^16.0 | Component testing |
| `playwright` | ^1.48 | E2E testing |
| `eslint` | ^9.0 | Linting |
| `prettier` | ^3.4 | Code formatting |
| `husky` | ^9.1 | Git hooks |
| `lint-staged` | ^15.2 | Staged file linting |
| `@commitlint/cli` | ^19.5 | Commit linting |

---

## 26. Development Roadmap

### 26.1 Phase Overview

```
P0  Infrastructure        ████████████  Week 1-2
P1  Authentication        ████████      Week 3-4
P2  Dashboard             ████████      Week 5-6
P3  System Management     ████████████  Week 7-8
P4  Product Module        ████████████  Week 9-10
P5  Inventory Module      ████████████  Week 11-12
P6  Order Module          ████████████  Week 13-14
P7  Purchase Module       ████████████  Week 15-16
P8  Warehouse Module      ████████████  Week 17-18
P9  Logistics Module      ████████      Week 19-20
P10 Finance Module        ████████      Week 21-22
P11 Notifications         ████████      Week 23-24
P12 Realtime              ████████      Week 25-26
P13 AI Copilot            ████████████  Week 27-30

Total: ~30 weeks (7.5 months)
```

### 26.2 Phase Details

| Phase | Story Points | Days | Dependencies |
|-------|-------------|------|--------------|
| P0 Infrastructure | 36 | 13 | None |
| P1 Authentication | 36 | 14 | P0 |
| P2 Dashboard | 30 | 11 | P1 |
| P3 System Management | 43 | 17 | P1 |
| P4 Product Module | 39 | 15 | P3 |
| P5 Inventory Module | 31 | 12 | P4 |
| P6 Order Module | 37 | 14 | P4, P5 |
| P7 Purchase Module | 44 | 17 | P4, P6 |
| P8 Warehouse Module | 45 | 17 | P5 |
| P9 Logistics Module | 31 | 12 | P6, P8 |
| P10 Finance Module | 34 | 13 | P6, P7 |
| P11 Notifications | 24 | 9 | P1 |
| P12 Realtime | 26 | 10 | P1, P11 |
| P13 AI Copilot | 40 | 15 | All |
| **Total** | **456** | **189** | |

### 26.3 Milestones

| Milestone | Target | Deliverable |
|-----------|--------|-------------|
| M0 | Week 2 | Dev environment, CI/CD, base infrastructure |
| M1 | Week 4 | Login, auth, route guards, permissions |
| M2 | Week 6 | Dashboard with KPIs and charts |
| M3 | Week 8 | User/role/permission/dictionary management |
| M4 | Week 10 | Full product CRUD with search |
| M5 | Week 12 | Inventory management with alerts |
| M6 | Week 14 | Order lifecycle with state machine |
| M7 | Week 18 | Purchase + warehouse operations |
| M8 | Week 22 | Logistics + finance modules |
| M9 | Week 26 | Notifications + real-time updates |
| M10 | Week 30 | AI copilot features |

### 26.4 Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Backend API delays | High | Mock APIs, OpenAPI spec-first development |
| Ant Design Pro breaking changes | Medium | Pin versions, test upgrades in staging |
| Complex state management | Medium | Strict state classification, clear boundaries |
| Performance with large datasets | High | Virtual lists, pagination, lazy loading |
| WebSocket reliability | Medium | Robust reconnection, offline queue |

---

## 27. Task Breakdown

### 27.1 Epic Overview

| Epic | Stories | Tasks | Priority |
|------|---------|-------|----------|
| E0: Infrastructure | 5 | 15 | P0 |
| E1: Authentication | 4 | 12 | P0 |
| E2: Dashboard | 4 | 10 | P1 |
| E3: System Management | 5 | 18 | P1 |
| E4: Product Module | 4 | 14 | P1 |
| E5: Inventory Module | 4 | 12 | P2 |
| E6: Order Module | 5 | 16 | P2 |
| E7: Purchase Module | 5 | 15 | P2 |
| E8: Warehouse Module | 4 | 14 | P2 |
| E9: Logistics Module | 4 | 10 | P3 |
| E10: Finance Module | 4 | 12 | P3 |
| E11: Notifications | 3 | 8 | P3 |
| E12: Realtime | 3 | 8 | P3 |
| E13: AI Copilot | 3 | 10 | P4 |

### 27.2 Sample Task Breakdown (E0: Infrastructure)

**Story E0-S1: Project Setup**
| Task | Priority | Points | Acceptance Criteria |
|------|----------|--------|---------------------|
| E0-S1-T1: Initialize Next.js 15 project | P0 | 2 | `npx create-next-app` with TypeScript, App Router, strict mode |
| E0-S1-T2: Configure TypeScript strict mode | P0 | 1 | `tsconfig.json` with `strict: true`, `noUncheckedIndexedAccess: true` |
| E0-S1-T3: Setup ESLint + Prettier | P0 | 1 | Lint passes, format consistent |
| E0-S1-T4: Configure Husky + commitlint | P0 | 1 | Pre-commit lint, commit message validation |
| E0-S1-T5: Setup path aliases | P0 | 1 | `@/` alias works for all imports |

**Story E0-S2: UI Framework Setup**
| Task | Priority | Points | Acceptance Criteria |
|------|----------|--------|---------------------|
| E0-S2-T1: Install Ant Design 5 | P0 | 1 | antd installed, theme provider configured |
| E0-S2-T2: Install Pro Components | P0 | 1 | ProTable, ProForm, ProLayout render correctly |
| E0-S2-T3: Configure theme tokens | P0 | 2 | Light/dark themes with CSS variables |
| E0-S2-T4: Create admin shell layout | P0 | 3 | Sidebar, header, content area, breadcrumbs |

**Story E0-S3: State Management Setup**
| Task | Priority | Points | Acceptance Criteria |
|------|----------|--------|---------------------|
| E0-S3-T1: Setup Zustand stores | P0 | 2 | AuthStore, UIStore, TenantStore created |
| E0-S3-T2: Setup TanStack Query | P0 | 2 | QueryClient configured with defaults |
| E0-S3-T3: Create providers | P0 | 2 | All providers composed in root layout |

**Story E0-S4: API Layer Setup**
| Task | Priority | Points | Acceptance Criteria |
|------|----------|--------|---------------------|
| E0-S4-T1: Create Axios instance | P0 | 2 | Base URL, timeout, headers configured |
| E0-S4-T2: Implement request interceptor | P0 | 2 | Auth token, tenant header injected |
| E0-S4-T3: Implement response interceptor | P0 | 3 | Error handling, 401 retry, toast messages |
| E0-S4-T4: Setup OpenAPI generator | P0 | 2 | Generator configured, types generated |

**Story E0-S5: CI/CD & Testing**
| Task | Priority | Points | Acceptance Criteria |
|------|----------|--------|---------------------|
| E0-S5-T1: Setup Vitest | P0 | 2 | Vitest configured, sample test passes |
| E0-S5-T2: Setup Playwright | P0 | 2 | Playwright configured, sample E2E passes |
| E0-S5-T3: Create GitHub Actions workflow | P0 | 3 | CI pipeline runs lint, test, build |
| E0-S5-T4: Create Dockerfile | P0 | 2 | Multi-stage build, production image works |
| E0-S5-T5: Create docker-compose.yml | P0 | 1 | Local dev environment starts |

### 27.3 Full Task List Summary

| Epic | Stories | Tasks | Total Points |
|------|---------|-------|-------------|
| E0: Infrastructure | 5 | 15 | 36 |
| E1: Authentication | 4 | 12 | 36 |
| E2: Dashboard | 4 | 10 | 30 |
| E3: System Management | 5 | 18 | 43 |
| E4: Product Module | 4 | 14 | 39 |
| E5: Inventory Module | 4 | 12 | 31 |
| E6: Order Module | 5 | 16 | 37 |
| E7: Purchase Module | 5 | 15 | 44 |
| E8: Warehouse Module | 4 | 14 | 45 |
| E9: Logistics Module | 4 | 10 | 31 |
| E10: Finance Module | 4 | 12 | 34 |
| E11: Notifications | 3 | 8 | 24 |
| E12: Realtime | 3 | 8 | 26 |
| E13: AI Copilot | 3 | 10 | 40 |
| **Total** | **57** | **174** | **456** |

---

## 28. Future Evolution

### 28.1 Micro Frontends

**When:** When team grows beyond 8 developers or modules need independent deployment.

**Approach:** Module Federation via Webpack 5 or Turbopack. Each business module becomes an independent Next.js app loaded by a shell application.

**Benefits:**
- Independent deployment per module
- Technology flexibility per module
- Team autonomy

**Challenges:**
- Shared state coordination
- Routing synchronization
- Bundle duplication

### 28.2 Module Federation

```
┌─────────────────────────────────────────────┐
│                Shell App                     │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐      │
│  │ Product │ │  Order  │ │Inventory│      │
│  │  (MF)   │ │  (MF)   │ │  (MF)   │      │
│  └─────────┘ └─────────┘ └─────────┘      │
│                                              │
│  Shared: React, Ant Design, Zustand         │
└─────────────────────────────────────────────┘
```

### 28.3 PWA (Progressive Web App)

**When:** When mobile access is needed without native app.

**Features:**
- Service worker for offline caching
- Push notifications
- Add to home screen
- Background sync

**Implementation:** Next.js PWA plugin + Workbox.

### 28.4 Electron Desktop

**When:** When desktop app is needed for warehouse/POS terminals.

**Approach:** Electron wrapper around Next.js app with:
- Native file system access
- System tray notifications
- Auto-update
- Offline mode with local SQLite

### 28.5 Mobile App

**When:** When mobile-native experience is required.

**Options:**
1. **React Native** — Share logic with web (hooks, services, schemas)
2. **Expo** — Faster development for React Native
3. **Capacitor** — Wrap existing web app as native app

**Shared code:** `features/*/hooks`, `features/*/services`, `features/*/schemas`, `types/`, `constants/`

### 28.6 AI Copilot

**When:** After core modules are stable (P13+).

**Features:**
- Natural language queries ("Show me orders from last week")
- Smart recommendations ("Reorder suggestions based on sales trends")
- Predictive analytics ("Demand forecasting for next month")
- Workflow automation ("Auto-approve orders under ¥1000")

**Architecture:**
```
User Input → LLM (GPT-4/Claude) → Intent Recognition → API Call → Response Generation → UI
```

### 28.7 Low-Code Platform

**When:** When business users need to create custom forms/reports.

**Features:**
- Drag-and-drop form builder
- Visual report designer
- Custom dashboard widgets
- Workflow designer

**Tech:** ReactFlow for visual editing, Monaco for expressions, custom DSL for business rules.

### 28.8 Workflow Designer

**When:** When approval flows need to be configurable by business users.

**Features:**
- Visual flow editor (ReactFlow)
- Condition nodes (if/else)
- Approval nodes (user/role/department)
- Notification nodes
- Parallel/sequential execution
- Version management

### 28.9 BI Dashboard

**When:** When advanced analytics are needed.

**Features:**
- Custom dashboard builder
- Drag-and-drop widgets
- SQL query builder (for power users)
- Scheduled reports
- Data export (Excel, PDF, CSV)
- Embeddable charts

**Tech:** ECharts for visualization, custom query builder, report scheduler.

### 28.10 SaaS Commercialization

**When:** When platform is mature enough for multi-customer deployment.

**Architecture changes:**
- Tenant-based billing
- Feature flag management per plan
- Usage metering
- Self-service tenant provisioning
- White-label support
- API marketplace

---

## Appendix A: ADR Index

| ADR | Decision | Date |
|-----|----------|------|
| ADR-001 | Next.js App Router over Pages Router | 2026-06-04 |
| ADR-002 | React over Vue | 2026-06-04 |
| ADR-003 | Ant Design Pro over raw Ant Design | 2026-06-04 |
| ADR-004 | TanStack Query for server state | 2026-06-04 |
| ADR-005 | Zustand for client state | 2026-06-04 |
| ADR-006 | Memory-only access tokens | 2026-06-04 |

## Appendix B: Glossary

| Term | Definition |
|------|------------|
| RSC | React Server Components |
| CSR | Client-Side Rendering |
| SSR | Server-Side Rendering |
| STOMP | Simple Text Oriented Messaging Protocol |
| RBAC | Role-Based Access Control |
| SPU | Standard Product Unit |
| SKU | Stock Keeping Unit |
| RFQ | Request for Quotation |
| BFF | Backend for Frontend |
| OpenAPI | Specification for describing REST APIs |
| TanStack Query | Data fetching and caching library (formerly React Query) |
| Zustand | Minimal state management library for React |
| ProTable | Ant Design Pro's enhanced table component |
| ProForm | Ant Design Pro's enhanced form component |
| ProLayout | Ant Design Pro's admin layout component |

---

*Document generated: 2026-06-04*  
*Status: Ready for review*  
*Next step: Implementation planning*
