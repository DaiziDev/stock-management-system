# Frontend Architecture — SGS Stock Management

> Frontend architecture for the SGS backend (Spring Boot 3.4.1 REST API, JWT security, multi-tenant by `entrepriseId`).
> Derived from a full backend analysis: 14 controllers, ~50 REST endpoints, 3 roles (ADMIN / GESTIONNAIRE / VENDEUR), no pagination today, single JWT (no refresh token).
> Companion documents: `CHG.md` / `CHG_EN.md` (functional requirements).
>
> **Adopted principles:** feature-first (each feature owns its api / hooks / schemas / stores / types / utils), locale-prefixed routes (`/fr`, `/en`) with role-grouped dashboard sections, **dark mode across the whole app**, **i18n across the whole app**, colocated `__tests__` folders.
> Stack: **React 19 + Vite + React Router v7** (SPA — no Next.js; the folder conventions below mirror an App-Router-style layout on purpose).

---

## 1. Backend Analysis Summary

### 1.1 What the API looks like

| Aspect | Observation | Frontend consequence |
|---|---|---|
| ~14 controllers under `/api/**` | auth, entreprises, utilisateurs, articles, categories, clients, fournisseurs, commandes-client, commandes-fournisseur, ventes, stock, mouvements-stock, notifications, dashboard | **One feature module per domain** (see §3) |
| **JWT stateless auth** | `Authorization: Bearer <token>` on everything except `/api/auth/login` + Swagger | Axios interceptor attaches token; 401 → logout to `/login` |
| **Single token, 24 h expiry, no refresh endpoint** | JWT holds `login`, `role`, `entrepriseId` | Decode JWT client-side for role/entreprise display; plan a refresh endpoint on the backend |
| **CORS locked to `http://localhost:4200` with `allowCredentials(true)`** | `CorsConfig.java` | Update backend allow-list to the React dev origin (e.g. `http://localhost:5173`) |
| **Multi-tenant filtering is server-side** | Every list is pre-filtered by the user's `entrepriseId` | No client-side tenant logic needed; render what the API returns |
| **Role authorization is NOT enforced per endpoint** | Only account management is ADMIN-gated | Frontend hides per role (§7), **backend must add real enforcement** |
| **No pagination/sorting anywhere** | Lists return everything, ordered by date desc | No table pagination lib for now; server pagination as an evolution item |
| **Transactionality is server-side** | All-or-nothing order validation / sale creation | Just surface the 409/400 message ("stock insuffisant") |
| **Statuses are life-cycle enums** (`EN_COURS`, `VALIDEE`, `ANNULEE`, `EN_ATTENTE`, `RECUE`…) | Not booleans | String-literal unions; label + color mapped once, labels resolved through i18n (§6) |
| **KPIs are precomputed server-side** (`/api/dashboard/kpis`) | 6 numbers, single snapshot | Dashboard fetches one endpoint; time-series endpoint needed for charts over time |

### 1.2 Key payloads the frontend will consume

- `POST /api/auth/login` → `{ token, user: { id, nom, prenom, login, role, entrepriseId } }`
- `GET /api/auth/me` → profile (rehydrates the session on page refresh)
- `GET /api/dashboard/kpis` → 6 numbers for the home page
- `GET /api/stock/alertes` + `GET /api/notifications` → low-stock badge in the navbar
- Orders/sales responses include `lignes[]` with `sousTotal` and a computed `total`
- Errors: `{ timestamp, status, error, message, path }` — business messages are currently in **French** (i18n strategy in §6 handles this)

---

## 2. Recommended Tech Stack

| Layer | Choice | Why this one (vs alternatives) |
|---|---|---|
| **Framework** | **React 19** + **Vite 6** + **TypeScript 5.x (strict)** | Vite = instant HMR, standard for new React apps. TypeScript is non-negotiable: backend DTO shapes are stable; strict types catch contract drift at compile time |
| **Routing** | **React Router v7 (library mode)** with `/:locale` path prefix | Recreates the `app/[locale]/(group)/section` URL scheme of the target layout: `/fr/dashboard/gestionnaire/articles`, `/en/login`… Lazy route code-splitting included |
| **State manager** | **Zustand** | Only *auth* and *UI prefs (theme)* are truly global. Redux would be overkill; Context re-renders everything. **Server data is NOT in Zustand** — that's TanStack Query's job |
| **Server state** | **TanStack Query v5** | The app is 90 % server data: caching, background refetch, retries, loading/error states — and the low-stock badge polls every 60 s with one option |
| **HTTP client** | **Axios** | Interceptors attach the JWT and handle 401 globally (`core/api/interceptors` — unit-tested) |
| **UI components** | **shadcn/ui** | Copy-in components (Radix + Tailwind), full ownership. Built on CSS variables → **theming/dark mode is first-class** |
| **Styling** | **Tailwind CSS v4** (class-based dark mode) | Pairs natively with shadcn/ui; `dark:` variants + CSS variables for theme tokens |
| **Icons** | **Lucide React** | Tree-shakeable, shadcn/ui's default, consistent stroke style |
| **Charts** | **Recharts 3** | Declarative React API for revenue trend (LineChart), low-stock top-N (BarChart), category split (PieChart). Colors read from CSS variables so **charts follow dark mode automatically** |
| **Forms** | **react-hook-form + zod** | Minimal re-renders on large order forms; one zod `schemas/` folder per feature; messages resolved through i18n (§6.5) |
| **i18n** | **i18next + react-i18next** | Industry standard: namespaces, interpolation with escaping, plurals, lazy-loaded bundles. Locale lives in the URL (§6.2) |
| **Dates & money** | **date-fns + Intl.NumberFormat** | Locale pulled from the URL locale (§6.4); money math stays on the backend |
| **Testing** | **Vitest + React Testing Library (+ MSW)** | Colocated `__tests__/` folders like the target layout: interceptors, hooks, schemas, utils are all unit-tested |

Versions: `react@19`, `vite@6`, `typescript@5`, `zustand@5`, `@tanstack/react-query@5`, `axios@1`, `react-router@7`, `tailwindcss@4`, `shadcn/ui`, `lucide-react`, `recharts@3`, `react-hook-form@7`, `zod@3`, `i18next@24`, `react-i18next@15`, `vitest@3`, `@testing-library/react@16`, `msw@2`.

---

## 3. Project Structure

**Three layers, strict dependency direction:**

```
app/ (routes only, thin) ──renders──> features/ (business logic) ──uses──> core/ + shared/ (infrastructure)
```

- `app/` = **URL structure only** — every page is a thin shell that re-exports from `features/`. No logic lives here.
- `features/` = business modules, fully self-contained (api, hooks, schemas, stores, types, utils, components).
- `core/` = framework-level infrastructure (http, providers, generic utils). Knows nothing about SGS business.
- `shared/` = business-aware reusable UI/state (DataTable, DashboardLayout, status badge…). Knows the domain, but no single feature.

```
sgs-frontend/
├── public/
│   ├── assets/
│   └── Logo/
├── scripts/                            # codegen (OpenAPI types, i18n key checks…)
└── src/
    ├── app/                            # ─── ROUTES (thin shells only) ───
    │   └── [locale]/                   #   locale prefix — validated by localeGuard
    │       ├── (auth)/                 #   PUBLIC group
    │       │   ├── login/
    │       │   ├── forgot-password/    #     planned — backend endpoint missing (§9)
    │       │   ├── change-password/    #     planned
    │       │   └── first-login-info/   #     planned
    │       ├── (dashboard)/            #   PROTECTED group — ProtectedRoute wrapper
    │       │   ├── dashboard/          #     KPI home (all roles)
    │       │   ├── account/            #     my profile (all roles)
    │       │   ├── admin/              #     ADMIN only — RoleGuard
    │       │   │   ├── utilisateurs/
    │       │   │   │   ├── new/
    │       │   │   │   └── [id]/
    │       │   │   ├── entreprises/
    │       │   │   │   └── [id]/
    │       │   │   └── settings/
    │       │   ├── gestionnaire/       #     GESTIONNAIRE only — RoleGuard
    │       │   │   ├── articles/
    │       │   │   │   ├── new/
    │       │   │   │   └── [id]/edit/
    │       │   │   ├── categories/
    │       │   │   ├── fournisseurs/
    │       │   │   ├── commandes-fournisseur/
    │       │   │   │   ├── new/
    │       │   │   │   └── [id]/
    │       │   │   ├── commandes-client/
    │       │   │   │   ├── new/
    │       │   │   │   └── [id]/
    │       │   │   ├── mouvements-stock/
    │       │   │   └── stock/
    │       │   └── vendeur/            #     VENDEUR only — RoleGuard
    │       │       ├── ventes/
    │       │       │   ├── new/
    │       │       │   └── [id]/
    │       │       └── clients/
    │       ├── (landing)/              #   PUBLIC marketing group (optional, low priority)
    │       │   ├── about/
    │       │   └── features/
    │       └── setup/                  #   first-run wizard (planned — see §9)
    │
    ├── config/                         # env parsing (zod), feature flags, API_BASE
    │
    ├── core/                           # ─── INFRASTRUCTURE (business-agnostic) ───
    │   ├── api/
    │   │   ├── httpClient.ts           #   axios instance
    │   │   ├── interceptors/
    │   │   │   ├── authInterceptor.ts  #   attach Bearer token
    │   │   │   ├── errorInterceptor.ts #   401 → logout, error normalize
    │   │   │   └── __tests__/
    │   │   └── __tests__/
    │   ├── constants/                  #   storage keys, regex, limits
    │   ├── hooks/                      #   useDebounce, useMediaQuery, useOnClickOutside
    │   ├── providers/                  #   QueryProvider, ThemeProvider, I18nProvider
    │   ├── services/                   #   tokenStorage (localStorage wrapper), errorMapper
    │   ├── types/                      #   ApiError, Paginated<T>, ApiResponse<T>
    │   └── utils/
    │       ├── dateUtils/   (+__tests__)
    │       ├── formatters/  (+__tests__)   # money/number via Intl
    │       └── __tests__/
    │
    ├── features/                       # ─── BUSINESS MODULES (self-contained) ───
    │   ├── auth/
    │   │   ├── api/           (+__tests__)
    │   │   ├── components/             #   LoginForm, ProtectedRoute, RoleGuard, RequirePermission
    │   │   ├── hooks/         (+__tests__)   #   useLogin, useMe, useLogout, usePermissions
    │   │   ├── schemas/       (+__tests__)   #   loginSchema (zod)
    │   │   ├── stores/        (+__tests__)   #   auth.store.ts (token + user)
    │   │   ├── types/
    │   │   ├── utils/         (+__tests__)   #   decodeJwt, isTokenExpired
    │   │   └── __tests__/
    │   ├── dashboard/
    │   │   ├── api/  components/  hooks/  types/
    │   │   └── (KpiCard, RevenueTrendChart, LowStockChart)
    │   ├── articles/          #   api/ components/ hooks/ schemas/ types/ (+__tests__)
    │   ├── categories/
    │   ├── clients/
    │   ├── fournisseurs/
    │   ├── orders/                     #   nested sub-features (like example's pedagogie/*)
    │   │   ├── client/                 #     api/ components/ hooks/ schemas/ types/
    │   │   └── supplier/               #     api/ components/ hooks/ schemas/ types/
    │   ├── sales/                      #   SaleForm (multi-line cart), SaleDetailDrawer
    │   ├── stock/                      #   StockTable, StockAlertBadge, ValorisationCard
    │   ├── movements/                  #   MovementHistoryTable, AdjustStockDialog
    │   ├── users/                      #   admin module (list, create, roles)
    │   ├── companies/                  #   admin module (entreprise CRUD)
    │   ├── notifications/              #   LowStockBell, useNotifications (60 s poll)
    │   └── settings/                   #   ThemeToggle, LanguageSwitcher, ProfilePage
    │
    ├── shared/                         # ─── REUSABLE, BUSINESS-AWARE ───
    │   ├── components/
    │   │   ├── ui/            (+__tests__)   #   shadcn primitives
    │   │   ├── layout/
    │   │   │   └── DashboardLayout/          #   Sidebar + Navbar + LowStockBell + ThemeToggle
    │   │   ├── charts/                       #   chart wrappers reading CSS variables
    │   │   ├── crud/                         #   DataTable, ConfirmDialog, FormDrawer, StatusBadge
    │   │   └── feedback/                     #   EmptyState, ErrorState, LoadingSkeleton
    │   ├── hooks/             (+__tests__)
    │   ├── stores/            (+__tests__)   #   ui.store.ts (theme, persisted)
    │   └── utils/             (+__tests__)
    │
    ├── i18n/
    │   ├── config.ts                   #   i18next init, namespace lazy-loading
    │   └── locales/
    │       ├── fr/                     #   common.json, auth.json, articles.json, orders.json, …
    │       └── en/                     #   same namespaces
    │
    ├── lib/                   (+__tests__)   # queryClient singleton, queryKeys helpers
    └── types/                          # global.d.ts, vite-env.d.ts
```

### 3.1 Canonical feature anatomy

Every feature follows the same skeleton — `auth` as the reference:

```
features/auth/
├── api/
│   ├── auth.api.ts
│   └── __tests__/auth.api.test.ts       # with MSW handlers
├── components/
│   ├── LoginForm.tsx
│   ├── ProtectedRoute.tsx
│   ├── RoleGuard.tsx
│   └── RequirePermission.tsx
├── hooks/
│   ├── useLogin.ts
│   ├── useMe.ts
│   ├── useLogout.ts
│   ├── usePermissions.ts
│   └── __tests__/useLogin.test.ts
├── schemas/
│   ├── login.schema.ts                  # zod — mirrors backend @NotBlank rules
│   └── __tests__/login.schema.test.ts
├── stores/
│   ├── auth.store.ts                    # Zustand: token + user
│   └── __tests__/auth.store.test.ts
├── types/
│   └── auth.types.ts                    # mirrors backend DTOs exactly
├── utils/
│   ├── jwt.ts
│   └── __tests__/jwt.test.ts
└── __tests__/                           # integration tests (component-level)
```

Feature code example:

```ts
// features/articles/api/articles.api.ts
import { httpClient } from "@/core/api/httpClient";
import type { Article } from "../types/article.types";

export const articlesApi = {
  list: () => httpClient.get<Article[]>("/api/articles").then(r => r.data),
  getById: (id: number) => httpClient.get<Article>(`/api/articles/${id}`).then(r => r.data),
  create: (body: ArticlePayload) => httpClient.post<Article>("/api/articles", body).then(r => r.data),
  update: (id: number, body: ArticlePayload) => httpClient.put<Article>(`/api/articles/${id}`, body),
  remove: (id: number) => httpClient.delete<void>(`/api/articles/${id}`),
};
```

```tsx
// app/[locale]/(dashboard)/gestionnaire/articles/index.tsx — THIN ROUTE SHELL
import { ArticlesPage } from "@/features/articles";

export default function ArticlesRoute() {
  return <ArticlesPage />;   // all logic lives in the feature
}
```

### 3.2 Rules of the layout

1. **Dependency direction**: `app/ → features/ → shared/ → core/`. Nothing imports upward (`core` never imports a feature; `shared` never imports `features/*` — cross-feature needs go through `shared/` or the feature's public `index.ts`).
2. **Route shells stay thin** — `app/**` files contain imports and layout wrappers only. Role folders (`admin/`, `gestionnaire/`, `vendeur/`) re-export the same feature pages; **no duplication of UI code**, duplication would only be route declarations.
3. **Every feature maps 1:1 to a backend controller** (`features/orders/client` ↔ `CommandeClientController`). A new endpoint has exactly one obvious home.
4. **`__tests__/` colocated** next to the code it tests (Vitest): api layers tested with MSW, hooks with `renderHook`, schemas and utils as pure unit tests, stores with direct store manipulation.
5. **Cross-feature imports go through each feature's `index.ts`** (enforced by ESLint `import/no-restricted-paths`). Example: `shared/components/layout` imports `{ useNotifications } from "@/features/notifications"`.
6. **i18n namespace ownership**: `features/articles` owns `locales/*/articles.json`. Adding a feature = adding its namespace file in both `fr/` and `en/`.

---

## 4. Routing & Guards

```
/:locale/…                          ← localeGuard: validates fr|en, i18n.changeLanguage(param)
    (auth)/login                    ← public; redirects to dashboard if already authed
    (dashboard)/…                   ← ProtectedRoute: requires a valid token (rehydrated via /api/auth/me)
        admin/**                    ← RoleGuard: ROLE_PERMISSIONS[role].pages includes /admin
        gestionnaire/**             ← RoleGuard: gestionnaire page list
        vendeur/**                  ← RoleGuard: vendeur page list
    (landing)/**                    ← public marketing pages
```

- Guards live in `features/auth/components` and are applied as layout routes in `app/[locale]` — one wrapper per group, not per page.
- Unknown role page access → redirect to `/:locale/dashboard` (not a 404 — the user *is* authenticated).
- A wrong-role API call still gets a proper 401/403 from the backend once enforcement lands (§9 item 1) — the client guard is UX only.

---

## 5. Dark Mode (whole app)

### 5.1 Mechanism

- Tailwind CSS v4 **class-based dark mode**: one `.dark` class on `<html>` switches every `dark:` variant.
- shadcn/ui tokens are **CSS variables** (light + dark sets declared once in `index.css`). Components only consume `bg-background`, `text-muted-foreground`, `border-border`… — dark mode comes free on every component.
- Three-state theme: **`light` | `dark` | `system`** (follows the OS, default). Persisted in `shared/stores/ui.store.ts` (Zustand + `persist`), independent of the URL locale.

### 5.2 Implementation

```ts
// shared/stores/ui.store.ts
import { create } from "zustand";
import { persist } from "zustand/middleware";

type Theme = "light" | "dark" | "system";

interface UiState {
  theme: Theme;
  setTheme: (t: Theme) => void;
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      theme: "system",
      setTheme: (theme) => set({ theme }),
    }),
    { name: "sgs-ui" }
  )
);
```

```ts
// core/providers/ThemeProvider.tsx — single place that touches the DOM
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const theme = useUiStore((s) => s.theme);

  useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const apply = () => {
      const dark = theme === "dark" || (theme === "system" && media.matches);
      document.documentElement.classList.toggle("dark", dark);
      document.documentElement.style.colorScheme = dark ? "dark" : "light";
    };
    apply();
    if (theme === "system") {
      media.addEventListener("change", apply);
      return () => media.removeEventListener("change", apply);
    }
  }, [theme]);

  return <>{children}</>;
}
```

### 5.3 Anti-FOUC

A tiny inline script in `index.html` applies the `.dark` class **before React mounts** (reads localStorage `sgs-ui` + `matchMedia`) — otherwise users see a white flash on dark theme at every reload.

### 5.4 Dark-mode checklist

| Concern | Rule |
|---|---|
| **Colors** | Only CSS-variable utilities (`bg-background`, `text-foreground`, `bg-card`…). A raw `bg-white`/`text-black` in a PR is a review fail |
| **Charts (Recharts)** | Pass colors from CSS variables (`stroke: "var(--chart-1)"`); charts re-theme instantly, no JS branching |
| **Icons/images** | Dark variants or `opacity`/`invert` for the logo; verify every screen in both modes |
| **Status badges** | `StatusBadge` uses token-based colors (`bg-success/15 text-success`) where `--success` has light/dark values |
| **Toggle UI** | `ThemeToggle` in `features/settings`, rendered in the `DashboardLayout` navbar (System / Light / Dark) |

---

## 6. Internationalization (whole app)

### 6.1 Stack & setup

**i18next + react-i18next**, configured in `src/i18n/config.ts`:

- Languages: **`fr` (default, fallback)** and **`en`**. The backend domain is French; fr is the source locale.
- **One namespace per feature** + shared ones: `common`, `auth`, `dashboard`, `articles`, `categories`, `clients`, `fournisseurs`, `orders`, `sales`, `stock`, `movements`, `users`, `companies`, `notifications`, `settings`, `errors`, `validation`.
- Namespaces **lazy-loaded** per route chunk — a user on the dashboard never downloads the sales translations.

### 6.2 Locale in the URL (like the target layout)

The URL is the source of truth for language — mirroring `app/[locale]/…`:

- `/fr/dashboard/gestionnaire/articles` and `/en/dashboard/gestionnaire/articles` render the same page in different languages.
- `localeGuard` (route loader on the `/:locale` layout) validates the param (`fr|en`), calls `i18n.changeLanguage(locale)`, sets `<html lang>`, and **redirects unknown locales** to the detected one.
- Visiting `/` redirects to `/{detected-locale}` (from `localStorage sgs-ui` → `navigator.language` → `fr`).
- `LanguageSwitcher` (in `features/settings`, navbar) navigates to the **same path with the other locale prefix** — no separate state to keep in sync; the choice also persists so future visits open in the right language.

### 6.3 Translation conventions

```jsonc
// i18n/locales/fr/orders.json — one file per feature, per language
{
  "client": {
    "title": "Commandes clients",
    "status": { "EN_COURS": "En cours", "VALIDEE": "Validée", "ANNULEE": "Annulée" },
    "actions": { "validate": "Valider la commande", "cancel": "Annuler la commande" },
    "confirmCancel": "Annuler la commande {{code}} ? Cette action est irréversible."
  },
  "supplier": {
    "title": "Commandes fournisseurs",
    "actions": { "receive": "Réceptionner" }
  }
}
```

Rules:
- **Never hardcode user-visible strings** — always `t("key")` within the feature's namespace.
- **Interpolation, not concatenation**: `t("confirmCancel", { code })`.
- Keys are **camelCase**, organized by UI area (`title`, `actions.*`, `fields.*`, `status.*`); nested sub-features (orders → `client` / `supplier`) mirror the feature folders.
- Plurals use i18next built-ins (`item_one` / `item_other`).

### 6.4 Statuses, dates & money

- `core/constants/status.ts` maps each backend enum to `{ tone, i18nKey }`:
  ```ts
  export const ORDER_STATUS_META: Record<StatutCommandeClient, StatusMeta> = {
    EN_COURS: { tone: "warning", i18nKey: "client.status.EN_COURS" },
    VALIDEE:  { tone: "success", i18nKey: "client.status.VALIDEE" },
    ANNULEE:  { tone: "danger",  i18nKey: "client.status.ANNULEE" },
  };
  // StatusBadge renders t(meta.i18nKey) → translated everywhere, themed via tokens
  ```
- **Dates**: `core/utils/dateUtils` wraps date-fns with the locale matching the URL locale (`fr` → `dateFns.fr`).
- **Money/numbers**: `core/utils/formatters` uses `Intl.NumberFormat(locale, { style: "currency", currency: "XAF" })` — one helper, used everywhere; totals are never recomputed client-side (backend computes them).

### 6.5 Backend errors & validation

- Backend business messages are **French strings** today ("Stock insuffisant pour l'article 'X'"). Strategy: map **known cases to i18n keys** by status/type (`409` stock → `errors.stockInsuffisant`, `404` → `errors.notFound`) in `core/services/errorMapper`, falling back to the raw `message` for unknown ones. (Long-term backend item: machine-readable error codes — §9.)
- **zod** schemas per feature (`features/*/schemas/`) hold message **keys** resolved through the `validation` namespace at render time — switching locale re-renders form errors instantly.

### 6.6 What is NOT translated

- Domain nouns that are API/DB identifiers (`codeArticle`, enum values in storage, codes `CC-000042`) — only their *labels* are translated.
- Emails sent by the backend (server-side concern, out of frontend scope).

---

## 7. Frontend Security

### 7.1 Authentication & token handling

```ts
// core/api/interceptors/authInterceptor.ts
import type { AxiosInstance } from "axios";
import { useAuthStore } from "@/features/auth";

export function attachAuthInterceptor(instance: AxiosInstance) {
  instance.interceptors.request.use((config) => {
    const token = useAuthStore.getState().token;   // read, don't subscribe
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });
}
```

```ts
// core/api/interceptors/errorInterceptor.ts
export function attachErrorInterceptor(instance: AxiosInstance) {
  instance.interceptors.response.use(
    (res) => res,
    (error) => {
      if (error.response?.status === 401) {
        useAuthStore.getState().clearSession();     // token expired/invalid
        window.location.assign("/login");           // hard nav clears all state
      }
      return Promise.reject(error);
    }
  );
}
```

```ts
// features/auth/stores/auth.store.ts
export const useAuthStore = create<AuthState>((set) => ({
  token: tokenStorage.get(),
  user: null, // rehydrated via GET /api/auth/me — never trusted from storage
  login: (token, user) => {
    tokenStorage.set(token);
    set({ token, user });
  },
  clearSession: () => {
    tokenStorage.clear();
    set({ token: null, user: null });
    queryClient.clear(); // evict ALL cached server data — mandatory on logout
  },
}));
```

- **`queryClient.clear()` on logout is mandatory** — TanStack Query caches per-key; without it, the next user on a shared machine could see the previous tenant's cached lists.
- **Session rehydration**: on app boot, if a token exists → `GET /api/auth/me`; 200 → restore user; 401 → clear and redirect. `isAuthenticated` must mean "a **valid** token exists", not just "a token string exists".
- **No refresh token exists** — until the backend adds `POST /api/auth/refresh`, a 401 means re-login. Avoid auto-retry loops.
- Token storage goes through `core/services/tokenStorage` (thin localStorage wrapper) so the storage decision is swappable in one file (memory/sessionStorage upgrade path).

### 7.2 Client-side authorization

- **Server is the source of truth.** The backend does not enforce role rules on business endpoints today; the frontend can only *guide*, not protect (§9 item 1).
- One declarative, auditable matrix drives both the route guards and the UI:

```ts
// core/constants/permissions.ts
export type Action =
  | "orders:validate" | "orders:cancel" | "orders:receive"
  | "sales:create" | "stock:adjust"
  | "users:manage" | "companies:manage";

export const ROLE_PERMISSIONS: Record<UserRole, { pages: string[]; actions: Action[] }> = {
  ADMIN:        { pages: ["/admin", "/dashboard", "/account"], actions: ["*"] },
  GESTIONNAIRE: {
    pages: ["/dashboard", "/account", "/gestionnaire"],
    actions: ["orders:validate", "orders:cancel", "orders:receive", "sales:create", "stock:adjust"],
  },
  VENDEUR:      {
    pages: ["/dashboard", "/account", "/vendeur"],
    actions: ["sales:create"],   // stock read-only
  },
};
```

- **Route guard**: `ProtectedRoute` (token) + `RoleGuard` (page list) — applied once per route group in `app/[locale]/(dashboard)`.
- **Action-level**: `<RequirePermission action="orders:validate">` hides buttons; `usePermissions()` for conditional logic (e.g. a VENDEUR sees the stock page without the *Ajuster* button).
- Hiding is UX, not security — the definitive fix is backend `@PreAuthorize`.

### 7.3 XSS, CSRF, and data protection

| Threat | Mitigation |
|---|---|
| **XSS** | React escapes by default; never `dangerouslySetInnerHTML` with API data. **CSP** header: `default-src 'self'; img-src 'self' data:; connect-src 'self' api.example.com; frame-ancestors 'none'` |
| **i18n injection** | i18next escapes interpolated values by default (`escapeValue: true` — keep it). Dynamic values enter only via `{{interpolation}}`, never through raw-HTML keys |
| **JWT in localStorage = XSS-stealable** | CSP + React escaping + dependency audit (`npm audit`, Renovate) + no third-party scripts. Storage is isolated in `tokenStorage` for a future ADR-driven swap |
| **CSRF** | Not applicable — stateless `Authorization: Bearer`, no auth cookies. If ever moving the JWT to a cookie: backend CSRF + SameSite=Strict |
| **Sensitive data in client** | Backend excludes passwords from responses. Never cache tenant data in `localStorage`; the Query cache is memory-only and cleared on logout |
| **Multi-tenant leak on logout** | `queryClient.clear()` in `clearSession()` (§7.1) |
| **Open redirects** | Validate `redirect` params (local paths only, starts with `/`); post-login nav always goes through a whitelist check |
| **Dependency supply chain** | `npm audit` in CI, Renovate/Dependabot, lockfile committed, review `postinstall` scripts |
| **Clickjacking** | Backend sets `X-Frame-Options: DENY` / CSP `frame-ancestors 'none'` |
| **Vite env vars** | Only `VITE_*` are exposed — **no secrets in the frontend, ever** (JWT key, SMTP creds stay server-side). Env parsing is validated in `src/config` with zod at boot |
| **Communication** | HTTPS in prod; Vite dev proxy (`/api` → `http://localhost:8081`) to avoid CORS friction in dev |

- Never log tokens or user data in production; error text from the backend is displayed via `errorMapper` + `t()` when known (§6.5) and sanitized.
- **JWT claims are not authorization** — the role in the token must be re-checked server-side on every write (once backend enforcement lands).

---

## 8. Testing Conventions

Colocated `__tests__/` folders (Vitest + React Testing Library):

| Layer | What to test | Tooling |
|---|---|---|
| `core/api/interceptors` | Token attached, 401 → clearSession + redirect, error normalization | Vitest + MSW |
| `features/*/api` | Correct URL/payload/headers per endpoint | MSW handlers |
| `features/*/hooks` | Query gating, cache invalidation on mutation success | `renderHook` + MSW |
| `features/*/schemas` | Valid/invalid payloads mirror backend validation rules | pure unit |
| `features/*/utils` | JWT decode/expiry, formatters, date logic | pure unit |
| `features/*/stores` | login/clearSession transitions, persistence key | direct store calls |
| `shared/components` | DataTable render, StatusBadge tone per enum, guard components | RTL |
| Route guards | Redirect behaviour per role | RTL + MemoryRouter |

Target: **100 % on `core/` + schemas + stores** (cheap, high value), pragmatic coverage on feature hooks; component snapshots are banned — assert on behaviour and roles (`getByRole`).

---

## 9. Related Backend Evolution Items

The frontend can only be as good as its API:

1. **Per-endpoint role enforcement** (`@PreAuthorize`) — the critical one (CHG §8).
2. **Refresh-token endpoint** for secure session renewal.
3. **Password management endpoints** (change / forgot-password) — the `(auth)` route group reserves these pages.
4. **Server-side pagination** on list endpoints (articles, mouvements, ventes).
5. **Update the CORS allow-list** to the real frontend origins; drop the pointless `allowCredentials(true)`.
6. **Machine-readable error codes** (e.g. `"code": "STOCK_INSUFFISANT"`) instead of French prose — makes error i18n exact instead of best-effort (§6.5).
7. **Time-series dashboard endpoint** (`/api/dashboard/ventes-par-jour?from=&to=`) — needed for charts over time; current KPIs are single-snapshot.
8. **File upload endpoint** (`POST /api/files`) — photo fields exist but nothing uploads to them.
9. Notifications are computed on the fly — polling every 60 s is fine; SSE/WebSocket later if needed.

---

*Generated from backend source analysis — September 2026.*
