# Functional Requirements Document — SGS (Stock Management System)

> Document generated from an analysis of the actual implemented source code (`backend`).
> Version: 1.0 — September 2026
> (English translation of `CHG.md`)

---

## 1. Project Overview

### 1.1 Objective

SGS is a **multi-company stock management application** delivered as a REST API.
It enables a company to manage its product catalog, stock levels, customers,
suppliers, orders (purchases and sales) and users, with full traceability of
every stock movement.

### 1.2 Implemented scope

| Domain | Status |
|---|---|
| Authentication & JWT security | ✅ Implemented |
| Multi-company support (multi-tenant) | ✅ Implemented |
| User & role management | ✅ Implemented |
| Categories & Articles | ✅ Implemented |
| Customers & Suppliers (records) | ✅ Implemented |
| Customer orders (with validation workflow) | ✅ Implemented |
| Supplier orders (with receiving) | ✅ Implemented |
| Counter sales | ✅ Implemented |
| Stock movements & adjustments | ✅ Implemented |
| Stock state, alerts, valuation | ✅ Implemented |
| In-app notifications (low stock) | ✅ Implemented |
| Automatic emails (orders) | ✅ Implemented (best-effort) |
| Dashboard (KPIs) | ✅ Implemented |
| Interactive API documentation (Swagger) | ✅ Implemented |

### 1.3 Tech stack

- **Java 17** — Spring Boot **3.4.1** (Web, Data JPA, Validation, Security, Mail)
- **PostgreSQL** — relational database
- **Spring Security + JWT** (jjwt 0.12.6) — stateless authentication
- **Lombok** — boilerplate reduction
- **springdoc-openapi 2.8.4** — Swagger UI documentation
- **Maven** (wrapper included) — build tool

---

## 2. Actors and Roles

Three roles exist (the `UserRole` enum):

| Role | Description | Access in the current implementation |
|---|---|---|
| **ADMIN** | Manages HIS OWN company: user accounts and settings, within his tenant only | Account creation (`/register`), user management within their company, editing their own company (`/entreprises/me`) |
| **GESTIONNAIRE** (Manager) | Manages orders, suppliers, stock and reports | Business access (articles, orders, stock, sales…) |
| **VENDEUR** (Salesperson) | Counter sales + viewing articles/customers | Business access (sales, consultations) |

> **Implementation note**: per-endpoint permissions are enforced server-side by
> `@PreAuthorize` on all 52 endpoints. The expressions are centralised in
> `SecurityRoles` (package `config`), which defines three levels: `ADMIN`
> (companies, user accounts), `GESTION` = ADMIN+GESTIONNAIRE (catalogue writes,
> orders, suppliers, stock movements, valuation, KPIs) and `TOUS` = all three
> roles (catalogue/client/stock reads, sales, notifications).
>
> The authoritative role is the one read **from the database** on every request
> (`JwtAuthFilter` uses the authorities on `UserDetails`), not the token's
> `role` claim: with no revocation mechanism, a 24 h token would otherwise let a
> demoted user keep their privileges until it expired. The claim remains in the
> token for frontend display purposes.

### 2.1 Multi-tenancy

- Every business entity (article, customer, order, sale, movement…) carries a
  reference to its **company** (`identreprise`).
- The connected user's company is derived from their **JWT** at request time
  (`CurrentUserService` component).
- **Isolation rule**: any resource belonging to another company is reported as
  *not found* (404), never as *forbidden* (403) — so as not to reveal the
  existence of other companies' data.

---

## 3. Security and Authentication

### 3.1 Principles

- **Stateless** API: no server-side sessions; every request must carry the JWT
  in the `Authorization: Bearer <token>` header.
- Passwords are **never stored in plain text**: **BCrypt** hashing on the
  backend.
- CSRF disabled (unnecessary for a stateless API without session cookies).

### 3.2 Authentication endpoints

| Endpoint | Method | Access | Description |
|---|---|---|---|
| `/api/auth/login` | POST | Public | Verifies login + password, returns an access token + a refresh token + user info |
| `/api/auth/refresh` | POST | Public | Exchanges a valid refresh token for a NEW access/refresh pair (rotation) — no credentials needed |
| `/api/auth/register` | POST | ADMIN | Creates a user account in the CALLER's company (hashed password). No entrepriseId in the request: the tenant is forced to the connected admin's own — an admin cannot create accounts in another company |
| `/api/auth/me` | GET | Authenticated | Returns the connected user's profile (derived from the token) |

### 3.3 JWT contents

Two tokens are issued at every login (and at every refresh — **rotation**:
the previous pair is replaced):

| Token | Lifetime | Contents | Usage |
|---|---|---|---|
| **Access token** | 15 min (default) | login, role, company ID | Sent in `Authorization: Bearer` on every request |
| **Refresh token** | 7 days (default) | login only + `typ=refresh` claim | Presented to `POST /api/auth/refresh` to get a new pair |

Security rules:
- The refresh token carries **neither role nor company ID**: this data is
  re-read from the database at every refresh — a role change takes effect on
  the next refresh.
- A refresh token presented as an access token is **rejected** by
  `JwtAuthFilter` (otherwise its 7-day validity would make it a super-token).
- The account is **re-read from the database** at every refresh: a disabled
  or deleted account can no longer refresh, even with a still-valid token.
- Lifetimes configurable via `JWT_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_MS`.

### 3.4 Bootstrap account (bootstrapping)

On first startup (empty database), an initializer (`DataInitializer`) creates:

- The company **"SGS Demo"** — only if no company exists yet
- An administrator: login **`ADMIN_LOGIN`** (default `admin@sgs.local`), password
  **`ADMIN_PASSWORD`** — **required environment variable**

> ⚠️ No default credentials are committed in the code: without
> `ADMIN_PASSWORD`, the application refuses to start. Copy `.env.example`
> to `.env` and fill in the secrets (`JWT_SECRET`, `DB_PASSWORD`,
> `ADMIN_PASSWORD`…). See §7 for the full list.

### 3.5 Login rate limiting (brute force protection)

`POST /api/auth/login` — the only public endpoint — is protected by
`LoginRateLimitFilter`: after **5 failed** login attempts from the same IP
within a **5-minute** window, every further attempt receives a **429**
(with a `Retry-After` header) without even reaching the password check. A
**successful** login resets the counter; once the window expires, counters
restart from zero (temporary block).

Only authentication failures (401/403) are counted: a legitimate user behind
a shared IP is not penalized as long as they eventually log in. Counters are
in-memory (single-node instance); move them to a shared store (Redis…) for a
multi-instance deployment. Tuning:
`app.rate-limit.login.max-failures` / `window-seconds`.

---

## 4. Functional Modules

### 4.1 Company management

| Endpoint | Access | Description |
|---|---|---|
| `POST /api/entreprises/register` | **Public** | SaaS onboarding: creates the company AND its ADMIN account in one request (name, email, phone required + admin block: nom, prenom, email=login, password ≥ 8). 409 if name or email already taken |
| `GET /api/entreprises/me` | Authenticated | Details of MY company (derived from the token) |
| `PUT /api/entreprises/me` | ADMIN | Update my company (email + phone required) |
| `DELETE /api/entreprises/me` | ADMIN | Self-destruct: deletes my company AND its user accounts. 409 if business data (articles, clients…) is still attached — purge business data first |

> **Enumeration endpoints are gone**: the former `GET /api/entreprises`
> (global list) and `GET/PUT/DELETE /{id}` (arbitrary id) have been removed.
> Even an ADMIN can neither list nor touch another tenant: the targeted id is
> always the token's own, never a client-supplied parameter.

A company has: a **unique name**, an **address** (street, city, postal code,
country), an **email** and a **phone number** (both required).

**Typical onboarding**: public registration → the created admin logs in with
his email → creates his GESTIONNAIRE/VENDEUR users via `/api/auth/register` →
every user (admin included) only ever sees their own company's data.

---

### 4.2 User management

Restricted to the **ADMIN** role, limited to users of **their own company**.

| Endpoint | Description |
|---|---|
| `GET /api/utilisateurs` | List users of my company |
| `GET /api/utilisateurs/{id}` | User details |
| `PUT /api/utilisateurs/{id}` | Update name, contact details and role |
| `DELETE /api/utilisateurs/{id}` | Delete a user |

Rules:
- The **login is unique**; changing it is not supported (nor is the password,
  which belongs to dedicated future endpoints).
- Creation goes exclusively through `POST /api/auth/register` (ADMIN only); the new account is always attached to the admin's own company.
- A user cannot see or manage accounts of another company.

---

### 4.3 Categories

Full CRUD on `/api/categories` (list, details, create, update, delete).

A category has: a **unique code** and a **designation**. It can group multiple
articles.

---

### 4.4 Articles

Full CRUD on `/api/articles`.

Article fields:

| Field | Rule |
|---|---|
| `codeArticle` | **Unique**, required |
| `designation` | Required |
| `prixUnitaireHt` | Purchase / excl. tax (HT) price, required |
| `tauxTva` | VAT rate, required |
| `prixUnitaireTtc` | Incl. tax (TTC) price (basis for sales), required |
| `photo` | Image reference/URL (optional) |
| `categorie` | Optional category assignment |
| `stockActuel` | **Never modified directly**: only a stock movement can change it (see §4.9) |
| `seuilMin` | Optional alert threshold — unset means "no alert for this article" |

---

### 4.5 Customers

Full CRUD on `/api/clients`.

Customer record: **last name**, **first name** (both required), address, photo,
email, phone. The customer is attached to the creating user's company and is
used by **customer orders** and **sales**.

---

### 4.6 Suppliers

Full CRUD on `/api/fournisseurs`.

Supplier record: **name** (required), address, email, phone. Attached to the
company. Used by **supplier orders**.

---

### 4.7 Customer orders

Endpoints on `/api/commandes-client`:

| Endpoint | Description |
|---|---|
| `GET /api/commandes-client` | List my company's orders |
| `GET /api/commandes-client/{id}` | Order details |
| `POST /api/commandes-client` | Create an order (initial status `EN_COURS` / in progress) |
| `PUT /api/commandes-client/{id}/valider` | Validate → triggers **stock OUT movements** |
| `PUT /api/commandes-client/{id}/annuler` | Cancel (only while `EN_COURS`) |

**Lifecycle:**

```
EN_COURS ──validate──> VALIDEE   (irreversible, stock OUT movements generated)
EN_COURS ──cancel────> ANNULEE   (no stock impact, nothing moved yet)
```

**Rules:**
- At creation, the **TTC price of each line is frozen** (snapshot): later price
  changes on the article do not affect the order.
- Order + lines creation is **transactional**: if an article does not exist,
  nothing is saved.
- Each order gets a **readable code** `CC-000001`, `CC-000002`…
- **Validation**: only an `EN_COURS` order can be validated; validation
  generates one **stock OUT movement per line** (BR-02). If **a single line**
  lacks stock, **the entire operation is rolled back** — no partial validation.
- **Cancellation**: only an `EN_COURS` order can be cancelled. A `VALIDEE`
  order cannot (it would require reverse movements — out of current scope).
- A **confirmation email** is sent to the customer at creation (BR-08,
  best-effort: a send failure never blocks the order).

---

### 4.8 Supplier orders

Endpoints on `/api/commandes-fournisseur`:

| Endpoint | Description |
|---|---|
| `GET /api/commandes-fournisseur` | List my company's orders |
| `GET /api/commandes-fournisseur/{id}` | Order details |
| `POST /api/commandes-fournisseur` | Create (initial status `EN_ATTENTE` / pending) |
| `PUT /api/commandes-fournisseur/{id}/receptionner` | Receive → **stock IN movements** |
| `PUT /api/commandes-fournisseur/{id}/annuler` | Cancel (only while `EN_ATTENTE`) |

**Lifecycle:**

```
EN_ATTENTE ──receive──> RECUE     (irreversible, stock IN movements generated)
EN_ATTENTE ──cancel───> ANNULEE   (no stock impact)
```

**Rules:**
- Order lines freeze the article's **HT price** at order time.
- Readable codes `CF-000001`, `CF-000002`…
- **Receiving**: only an `EN_ATTENTE` order can be received; a `RECUE` order
  **cannot** be received twice (stock would be counted twice). Each line
  generates a **stock IN movement** (BR-03).
- A **purchase order email** is sent to the supplier at creation (BR-07,
  best-effort).

---

### 4.9 Sales

Endpoints on `/api/ventes`:

| Endpoint | Description |
|---|---|
| `GET /api/ventes` | List my company's sales |
| `GET /api/ventes/{id}` | Sale details |
| `POST /api/ventes` | Record a sale (immediate stock OUT movements) |

**Rules:**
- A sale **decrements stock immediately** upon creation: no validation step,
  unlike customer orders.
- **No update or delete**: a recorded sale is a historical fact; sales history
  is never rewritten.
- The customer is **optional**: anonymous sales are possible ("Client
  comptoir" / counter customer).
- Lines freeze the **TTC price** at sale time.
- Readable codes `VT-000001`, `VT-000002`…
- If any line lacks stock, **the whole sale is rejected** (transactional
  rollback) — no partial sales.
- Each sale generates a `SORTIE` (OUT) stock movement traced with its origin
  (`VT-xxxxxx`).

---

### 4.10 Stock — state, alerts, valuation

Endpoints on `/api/stock` (read-only — stock state is a **derived view** of
articles, not independent data):

| Endpoint | Description |
|---|---|
| `GET /api/stock/etat` | Stock state: all my company's articles with current stock and threshold |
| `GET /api/stock/alertes` | Articles whose stock is **≤ minimum threshold** configured |
| `GET /api/stock/valorisation` | Total stock value: Σ (current stock × HT unit price) |

---

### 4.11 Stock movements

Endpoints on `/api/mouvements-stock`:

| Endpoint | Description |
|---|---|
| `GET /api/mouvements-stock?articleId=&type=` | Movement history (optional filters by article and type) |
| `POST /api/mouvements-stock` | Create a **manual adjustment** |

**Three movement types:**

| Type | Stock effect | Origin |
|---|---|---|
| `ENTREE` (IN) | `stock += quantity` | Automatic — supplier order receiving |
| `SORTIE` (OUT) | `stock -= quantity` | Automatic — customer order validation or sale |
| `AJUSTEMENT` (ADJUSTMENT) | `stock += signed quantity` (+/-) | **Manual only** — inventory correction |

**Fundamental rules (traceability):**
- **No business code modifies `Article.stockActuel` directly**: only the stock
  movement service may do so, and always while writing an `MvtStk` trace in the
  same transaction (BR-04). No stock change can therefore happen without
  history.
- Each movement records a **snapshot of the stock after the movement**: the
  history stays accurate even as stock evolves afterwards.
- A movement may carry a textual **origin** (e.g. `CC-000042`, `VT-000012`)
  identifying the order or sale that triggered it.
- **Manual adjustment** requires a **reason** (BR-06) and accepts a negative
  quantity (downward correction). Automatic IN/OUT movements have no reason.
- Any operation that would result in a **negative stock** is rejected
  (`StockInsuffisantException`) and rolls back the calling transaction.

---

### 4.12 In-app notifications

| Endpoint | Description |
|---|---|
| `GET /api/notifications` | List of active alerts |

Only trigger implemented (BR-09): **stock reaching or dropping below the
minimum threshold**. Alerts are **computed on the fly** (no `Notification`
entity, no read/unread state): type `STOCK_BAS` (low stock), readable message,
article ID and designation.

---

### 4.13 Dashboard

| Endpoint | Description |
|---|---|
| `GET /api/dashboard/kpis` | Key indicators for the connected company |

Returned KPIs:

| KPI | Definition |
|---|---|
| Stock value | Σ (current stock × HT price) |
| Articles in alert | Number of articles below their minimum threshold |
| Customer orders in progress | Status `EN_COURS` |
| Pending supplier orders | Status `EN_ATTENTE` |
| Sales this month | Number of sales since the 1st of the month |
| Revenue this month | Σ of the month's sale totals |

---

### 4.14 Automatic emails

| Event | Email | Recipient |
|---|---|---|
| Customer order creation (BR-08) | Order confirmation (HTML) | Customer's email |
| Supplier order creation (BR-07) | Purchase order (HTML) | Supplier's email |

Rules:
- **Best-effort** sending: an SMTP failure is logged but **never blocks** order
  creation.
- No email on the record → nothing is sent (and no error).
- SMTP configuration via the `MAIL_USERNAME` / `MAIL_PASSWORD` environment
  variables (empty by default — without configuration, sends fail silently).

---

## 5. Business Rules (summary)

| Ref. | Rule |
|---|---|
| BR-02 | Customer order validation generates one stock OUT movement per line |
| BR-03 | Supplier order receiving generates one stock IN movement per line |
| BR-04 | No stock variation without a traced stock movement (`MvtStk`) |
| BR-06 | Every manual stock adjustment requires a reason |
| BR-07 | A purchase order email is sent to the supplier at creation |
| BR-08 | A confirmation email is sent to the customer at order creation |
| BR-09 | Stock reaching the minimum threshold triggers an in-app notification |
| — | Stock is never negative: any operation that would cause it is rejected entirely |
| — | Prices frozen (snapshot) on order/sale lines at operation time |
| — | Readable sequential codes: `CC-xxxxxx`, `CF-xxxxxx`, `VT-xxxxxx` |
| — | Multi-tenant isolation: other companies' data is "not found" (404) |
| — | Atomic transactions on multi-line operations (create, validate, receive, sell) |
| — | Sales are immutable (no update/delete) |

---

## 6. Main Business Flows

### 6.1 Replenishment (purchasing)

```
Create supplier record → Create supplier order (EN_ATTENTE / pending)
    → purchase order email sent to supplier
    → Receive (PUT /receptionner): stock IN per line (RECUE / received)
    → Available stock ↑
```

### 6.2 Sale via customer order

```
Create customer record → Create customer order (EN_COURS / in progress)
    → confirmation email sent to customer
    → Validate (PUT /valider): stock OUT per line (VALIDEE / validated)
    → Available stock ↓
```

### 6.3 Counter sale

```
Record the sale directly (POST /api/ventes)
    → immediate stock OUT movements
    → (customer optional)
```

### 6.4 Inventory correction

```
GET /api/stock/etat (observe discrepancy)
    → POST /api/mouvements-stock {articleId, signed quantity, reason}
    → stock corrected + historical trace
```

---

## 7. Technical Conventions

- **Database**: schema managed by Hibernate (`ddl-auto: update`) — tables are
  created/updated automatically at startup.
- **Configuration**: `application.yaml` with environment variables (`DB_URL`,
  `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`,
  `JWT_REFRESH_EXPIRATION_MS`,
  `ADMIN_LOGIN`, `ADMIN_PASSWORD`, `ADMIN_ROLE`, `CORS_ALLOWED_ORIGINS`,
  `MAIL_USERNAME`, `MAIL_PASSWORD`…). Template: `.env.example`.
  The secrets **`JWT_SECRET`, `DB_PASSWORD` and `ADMIN_PASSWORD` are required**
  (no default value): the application refuses to start without them. The JWT
  secret must be a Base64 key of at least 32 bytes (`openssl rand -base64 48`).
- **`.env` file (local development)**: via `spring-dotenv`, a `.env` file at
  the backend root feeds these variables without manual exports. It is
  **git-ignored**; copy `.env.example` to `.env` and fill in the values. Real
  environment variables keep priority (production behavior unchanged, where
  the file is simply absent).
- **Server port**: `8081`.
- **Interactive documentation**: Swagger UI at `/swagger-ui.html`, OpenAPI
  schemas at `/api-docs` and `/v3/api-docs` (public access).
- **Business errors**: 404 for missing or out-of-company resources, 400 for
  violated business rules (invalid status, insufficient stock…), 403 for
  actions reserved to a higher role.

---

## 8. Out of Current Scope (potential evolutions)

Elements explicitly not implemented so far (mentioned in the code):

- **Cancelling an already validated/received order** (would require reverse
  movements / credit notes).
- **Updating or deleting a sale** (deliberate choice: immutability).
- **Password management** (change, email-based reset) — the email sending
  service already exists and can serve as a basis.
- **Real photos** for articles/customers (field present, upload not handled).
- **Read/unread state and notification history** (computed on the fly).
- **Email templates** via a template engine (HTML built inline).
