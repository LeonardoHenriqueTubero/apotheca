# CLAUDE.md

Guidance for Claude Code (and any AI assistant) working in this repository.
Keep this file short and current. Detailed rationale for each decision lives in `docs/decisions/` (ADRs).
When a decision changes, update this file **and** add/supersede an ADR.

## Project

**Apotheca**: a home medication inventory manager (the name is decided; see decision #21).
A web app + PWA to track the medicines a household owns: what exists, how much is left,
where it is stored, and when it expires.

The real-world problem: at home there are many medicines and no control, so people think they
have a medicine when they don't, or keep medicines that expired years ago.

Goals:
1. Solve that problem for the developer's family and friends (phones via PWA, and browsers).
2. Be a **portfolio project for a junior developer seeking a first job**: production-like stack,
   automated tests, CI, and documented decisions. Explaining *why* a choice was made matters
   more than using the most powerful tool.

Not a medical product: it only tracks stock. It must never recommend doses or treatments.

## Language and communication

- Talk to the developer in **Brazilian Portuguese**. The developer is a junior learning the stack:
  explain concepts simply, with a short example, before assuming knowledge.
- Code, comments, commit messages, ADRs and READMEs: **English**.
- `README.md` (English, primary) and `README.pt-BR.md` (mirror). Keep both short
  (what it is, status, planned features, architecture, stack, ADR list, how to run; screenshot
  and demo link once they exist). Details go in ADRs (English only). The README is portfolio
  material: never describe a planned feature as done.
- **Rule:** update the README "Project status" checklist and the ADR table, in **both**
  languages, in the same PR that completes a roadmap step or adds an ADR.
- UI text: **pt-BR**.

## Stack (decided)

| Area | Choice |
|---|---|
| Frontend | Angular (recent version, standalone components), installable **PWA** |
| UI library | **Angular Material** (Material 3 theming via CSS variables); visual identity in ADR 0005 |
| Backend | Java 21, Spring Boot **4.1**, **Maven** (through the Maven Wrapper, `./mvnw`) |
| Database | PostgreSQL (**Neon** free plan in production, Docker Compose locally) |
| Migrations | **Flyway** |
| Auth | **Firebase Authentication**; Angular sends the JWT, Spring Security validates it (resource server) |
| Push notifications | **Firebase Cloud Messaging** (on iOS only works when the PWA is installed) |
| Hosting | Frontend: Firebase Hosting. API: Docker container on **Render** free plan. |
| Containers | **Docker from day one** (same image must run on Render and, later, Cloud Run) |
| Tests | JUnit + **Testcontainers** (real PostgreSQL, no H2) |
| CI | **GitHub Actions**, one workflow per app filtered by path: `backend-ci.yml` (`./mvnw verify`), `frontend-ci.yml` (`ng lint`, `ng test`, `ng build`); see ADR 0004 |
| Lint (frontend) | **ESLint** via `angular-eslint`, default rules from the Angular CLI (see ADR 0004) |
| Scheduled alerts | GitHub Actions **cron** (daily) calling a protected endpoint (not `@Scheduled`); per-user frequency, see ADR 0008 |
| API documentation | **springdoc-openapi** (Swagger UI), generated from the Spring Boot code |
| Mapping | DTOs as Java records, **MapStruct** for entity ↔ DTO, Lombok on JPA entities only |

Future option (not now): migrate the API to Google Cloud Run, only if a billing account is available.
Record it as an ADR when it happens ("why we moved from Render").

## Repository layout

```
/backend            Spring Boot API
/frontend           Angular app
/docs/decisions     ADRs (one file per decision or group of related decisions, English)
/docs/diagrams      Mermaid diagrams (architecture, ER, alert flow)
/.github/workflows  CI and scheduled jobs
docker-compose.yml  Local PostgreSQL
CLAUDE.md
README.md / README.pt-BR.md
```

Backend packages (package-by-layer, see ADR 0001):

```
br.dev.leonardo.apotheca
├── entity        JPA entities
├── dto           Request/response records
├── mapper        MapStruct mappers (entity ↔ DTO)
├── repository    Spring Data JPA interfaces
├── service       Business rules, transactions
├── controller    REST endpoints (DTOs only, never entities)
├── config        Spring configuration
└── exception     Custom exceptions + @RestControllerAdvice
```

Frontend folders (organized by feature, see ADR 0002):

```
frontend/src/app
├── core          App-wide singletons: auth guard, JWT interceptor
├── shared        Reusable components/pipes without business logic; models/ mirrors backend DTOs
├── features      One folder per domain (medication, household, auth...), each with its own service
└── app.routes.ts All routes; features lazy loaded with loadComponent
```

State lives in services with signals (no NgRx). A feature never imports another feature.

Monorepo on purpose: one PR can change API and UI together, one history, one place for docs.

## Git workflow and license (see ADR 0009)

- `main` is always stable. Work on short-lived branches: `feature/...`, `fix/...`
  (and `docs/...`, `chore/...`). Merge only through a pull request, after CI passes.
- Commit messages follow **Conventional Commits** (`feat:`, `fix:`, `docs:`, `chore:`, `ci:`,
  `test:`, `refactor:`, optional scope such as `feat(backend):`).
- License: **MIT** (`LICENSE` at the root).

## Non-negotiable rules

1. **Schema changes only via Flyway migration files** in
   `backend/src/main/resources/db/migration/` (`V1__...`, `V2__...`), committed to Git.
   Never run DDL directly on any database. Never edit a migration that was already applied;
   create a new one.
2. **MCP and AI integrations: read and diagnose freely, change through Git.**
   - Neon MCP: read-only, dev branches only. Never write to production.
   - Render MCP: logs, metrics, diagnosis. Infrastructure lives in a versioned `render.yaml`.
   - Firebase MCP: use a separate **development** Firebase project, never the family one.
   - Approve every tool call manually. Do not use Supabase (not part of the stack).
3. No secrets in the repo. Use environment variables / platform secrets.
4. Every backend feature comes with tests. Prefer Testcontainers over mocks for repository code.
5. The alert endpoint must be protected (secret token), since it is triggered by the CI cron.
6. Free-tier constraint: do not add paid services. If something needs a card or costs money,
   stop and ask the developer first.
7. Health-related data is sensitive under Brazilian privacy law (LGPD). Do **not** record
   *who* took a medicine in the MVP.

## Domain model (ER diagram in `docs/diagrams/er.md`, conventions in ADR 0011)

- `users` (identified by Firebase UID)
- `households`: the unit that owns medicines (family, friend's home). Users may belong to several.
- `household_members`: user + household + role (`owner` | `member`).
  Only the owner invites and removes members; removing is a `DELETE` here (ADR 0006).
- `household_invites`: `household_id`, `token` (unique, indexed), `created_by`, `expires_at`
  (24h), nullable `used_at` / `used_by`. Single use; joins as `member` (ADR 0006)
- `storage_locations`: "bathroom cabinet", "bag" (per household; cannot be deleted while it holds batches)
- `medications` (per household, no global catalog): name, active ingredient, strength, form (tablet, syrup...), unit,
  optional `shelf_life_after_opening_days`, optional `minimum_quantity` (low stock, ADR 0007)
- `batches`: one bought box. Expiration date, current quantity, location,
  optional `opened_at`
- `stock_movements`: history per batch (do not just overwrite quantity); `type` (`INITIAL` | `USE` |
  `DISCARD` | `ADJUSTMENT`) and signed `quantity_change`. Creating a batch writes `INITIAL`, so
  `sum(quantity_change) = current_quantity`. No user column (rule 7)
- `fcm_device_tokens`: `user_id`, `token` (unique), `platform` (`web` | `android` | `ios`),
  `created_at`, `last_used_at` (ADR 0008)
- `notification_preferences`: `user_id`, `household_id` (unique pair), `frequency`
  (`off` | `daily` | `weekly`, default `daily`), nullable `last_sent_at` (ADR 0008)

Key business rule: **effective expiry = the earlier of the printed expiry and
`opened_at + shelf_life_after_opening_days`** (syrups, eye drops, reconstituted antibiotics).
Pure logic, cover with unit tests. The app only stores what the user types from the leaflet.
Boundaries: a batch is usable through its effective expiry day and **expired** from the next day;
**expiring soon** when the effective expiry is within the next 30 days, inclusive. Implemented in
`ExpiryService` and `LowStockService`.

**Low stock** (ADR 0007): only for medications with `minimum_quantity` set; low when the sum of
`current_quantity` of its **active** batches (quantity > 0 and not past the effective expiry)
is below `minimum_quantity`. Pure logic, unit tested.

**Alerts** (ADR 0008): the cron runs daily; a push is sent only when the user's frequency for that
household says it is time (`last_sent_at`) **and** there is something to report (expired,
expiring within 30 days, or low stock). Never send an empty "all good" push.

Medication vs batch is deliberate: two boxes of the same medicine can have different expiries.

Conventions (ADR 0011): `BIGINT` identity ids; every request checks household membership;
hard delete with `ON DELETE CASCADE`; quantities `NUMERIC(10,2)` / `BigDecimal`; enums as
uppercase `VARCHAR` + `CHECK` with `@Enumerated(STRING)`. Dates per ADR 0010.

## MVP scope

- Register a medication with batch, expiry, quantity and storage location
- List with status: **expired**, **expires within 30 days**, **low stock**
- Decrease quantity (writes a stock movement)
- Push alerts via FCM, triggered by the scheduled endpoint
- Household creation and member access

## Roadmap after the MVP

- **v1.1**: price per batch (`purchase_price`, `purchased_at`, `initial_quantity`), monthly spending
  report, and *wasted value* (price share of expired, unused stock)
- **v2**: per-member reviews of a medication ("worked / didn't"), as a personal opinion,
  stored as `medication_reviews` (member, medication, rating, note). Never medical advice.
  Also an optional "what we use it for" note per medication (`medications.purpose`, e.g.
  "headache"), written by the family, never supplied by the app (no leaflet/ANVISA data).
- **Ideas**: barcode scanning, autocomplete from ANVISA open data, shopping list,
  guidance on correct disposal of expired medicines

## UX and design notes

- Mobile first; the app is used on phones.
- Status (expired / expiring / ok) must never rely on color alone: also use icon and text.
- Render free plan cold start is ~1 minute: the UI should show a "waking up the server" state.
- Visual identity (ADR 0005): calm / trustworthy. Primary teal `#1F6F6B` (light) / `#7FD9D1` (dark).
  Status colors, light / dark: expired `#B3261E` / `#FF9C8D`; expiring soon or low stock
  `#8A5A00` / `#FFC876`; ok `#2E7D53` / `#8FE3B0`. Fonts: **Newsreader** (titles, highlights) +
  **Work Sans** (body). Dark mode from the start. Implemented as an Angular Material M3 theme
  plus CSS variables; components use tokens, never hard-coded colors.
  Reference mockup: https://claude.ai/artifact/5EUGTb5cb1cfaaQMK2cket

## Diagrams

Diagrams are written in **Mermaid** inside Markdown files under `docs/diagrams/`
(GitHub renders them natively). Text in Git means history, easy edits by Claude Code,
and no loose images that nobody updates. Keep them few and small:

1. **Architecture** (first, decisions are already closed): Angular on Firebase Hosting, API on Render,
   database on Neon, Firebase Auth and FCM, and the GitHub Actions cron calling the API.
   Embed it in `README.md` and `README.pt-BR.md`.
2. **ER diagram** (households, members, storage locations, medications, batches, stock movements,
   device tokens): draw it **after** the open model decisions are closed and **before** writing `V1`,
   so it is not drawn twice.
3. **Alert flow, sequence diagram** (cron, protected endpoint, query of expiring batches, FCM push):
   draw it when the feature is implemented, so it shows what really exists.

Do not add more (for example a login diagram) unless something is genuinely hard to explain.
**Rule:** update the diagram in the same PR that changes the architecture or the schema.

## Open decisions (do not assume, ask the developer)

- Generated Angular client from the OpenAPI contract (Swagger UI itself is decided, see Stack; the generated client is still open)
- Demo mode or demo account so recruiters can try the app
- Confirm Firebase Hosting works without a card when we reach the first deploy
- Ownership when the only owner deletes their account or leaves a household (ADR 0011):
  transfer to another member (which one? oldest `joined_at` is the leading option) or block it;
  the household is deleted if no members remain. Decide with account deletion, record as a new ADR

## Suggested order of work

1. Create the repo and skeleton (monorepo, docker-compose with Postgres, Spring Boot, Angular, first CI)
2. Close the open decisions above; write the first ADRs from the decision log below;
   draw the architecture diagram
3. ER diagram, then the `V1` migration and domain model with tests
4. Auth (Firebase + Spring Security)
5. **First deploy early** (hello-world to Render, Neon and Firebase Hosting) to find hosting issues sooner
6. CRUD for medications and batches, status logic, stock movements
7. Alerts (cron workflow + FCM), then the alert-flow sequence diagram

## Decision log (rationale in one line each; expand into `docs/decisions/`)

| # | Decision | Why |
|---|---|---|
| 1 | Portfolio-first, free tier only | Developer is looking for a first job and has no budget |
| 2 | Angular + Spring Boot + PostgreSQL | Robust stack close to what the market uses |
| 3 | PWA instead of native apps | One codebase for phones and browsers; Capacitor possible later |
| 4 | Firebase Auth + FCM | Avoids storing passwords; push works through Firebase |
| 5 | Firebase Hosting has no Java runtime | Only static files there, so the API runs elsewhere |
| 6 | Render (API) + Neon (DB) | No card needed; Neon's free DB does not expire, Render's does after 30 days |
| 7 | Docker from day one | Same image on Render now and Cloud Run later; a migration ADR is good portfolio material |
| 8 | Cron via GitHub Actions, not `@Scheduled` | Free instances scale to zero, so an in-process scheduler may not run |
| 9 | Monorepo | Atomic changes, single history, easy for recruiters to browse |
| 10 | Maven | Most common in job postings; developer already knows it |
| 11 | English code/docs, pt-BR UI, bilingual short README | Recruiters read English, family uses Portuguese |
| 12 | Testcontainers over H2 | Tests run against real PostgreSQL, same as production |
| 13 | GitHub Actions CI | Automatic build/test, "build passing" badge (see ADR 0004) |
| 14 | Flyway, schema only by migration files | All databases stay identical; history is in Git |
| 15 | Households + medication/batch split + stock movements | Multi-home support, different expiries per box, usage history |
| 16 | Opening-shelf-life support | Real cases: syrups, eye drops, reconstituted antibiotics |
| 17 | Price in v1.1, reviews in v2 | Keep the MVP lean; reviews are subjective and touch sensitive data |
| 18 | MCP: read/diagnose only, changes through Git | Avoids schema drift and accidental production changes |
| 19 | Three Mermaid diagrams (architecture, ER, alert flow) | Recruiters read the README first; text diagrams stay versioned and updatable |
| 20 | springdoc-openapi (Swagger UI) in the backend | Free, generated from code, lets endpoints be tested from the browser before the frontend exists; good portfolio signal |
| 21 | App name: Apotheca | Short, memorable, evokes the traditional home medicine cabinet; replaces the working title |
| 22 | Package-by-layer: `entity`, `dto`, `mapper`, `repository`, `service`, `controller`, `config`, `exception` | Small MVP; most common layout at junior level, easy to explain in interviews (see ADR 0001) |
| 23 | DTOs as Java records, no Lombok on DTOs | Records are immutable and concise; Lombok is not needed for DTOs (see ADR 0001) |
| 24 | MapStruct for entity ↔ DTO mapping, in its own `mapper` package | Avoids hand-written mapping; compile-time checked; common in Spring job postings (see ADR 0001) |
| 25 | Lombok on JPA entities: `@Getter`/`@Setter`/`@NoArgsConstructor` only, no `@Data`/`@ToString`/`@EqualsAndHashCode` | Entities must be mutable for Hibernate; avoids equals/hashCode/toString issues with lazy relationships (see ADR 0001) |
| 26 | Base package `br.dev.leonardo.apotheca` | Reverse-domain naming convention; no registered domain of our own |
| 27 | Frontend organized by feature: `core/`, `shared/` (with `models/`), `features/` | Recommended by the Angular docs; one feature per folder, easy to find and delete (see ADR 0002) |
| 28 | Routes in `app.routes.ts`, features lazy loaded with `loadComponent`, no `NgModule` | Standalone components are the default; smaller initial bundle on phones (see ADR 0002) |
| 29 | State in services with signals, no NgRx | Built into Angular; NgRx adds complexity the MVP does not need (see ADR 0002) |
| 30 | UI library: Angular Material (Material 3); visual identity defined in ADR 0005 (#33-36) | Official, standalone, accessible; M3 CSS variables carry the identity; most recognized in job postings (see ADR 0003) |
| 31 | Frontend CI: own workflow `frontend-ci.yml`, filtered by `frontend/**`, own badge; `npm ci` → `ng lint` → `ng test` → `ng build` | Same pattern as the backend; a change on one side does not run the other's pipeline (see ADR 0004) |
| 32 | ESLint on the frontend via `angular-eslint`, default rules, run in CI | Catches common mistakes before merge; defaults avoid debating rules before there is code (see ADR 0004) |
| 33 | Visual identity: calm / trustworthy, teal primary `#1F6F6B` / `#7FD9D1` | Health data calls for a quiet interface; color is reserved for status (see ADR 0005) |
| 34 | Status colors (expired, expiring soon / low stock, ok) with light and dark values, always icon + text + color | Status is the key information; accessible to color-blind users (see ADR 0005) |
| 35 | Typography: Newsreader (titles) + Work Sans (body) | Calm editorial tone plus legible body text on phones; free Google Fonts (see ADR 0005) |
| 36 | Dark mode from the start, identity as M3 theme + CSS variable tokens | Cheap now, expensive later; one place to change colors (see ADR 0005) |
| 37 | Household invite: shareable link via `navigator.share()`, random token, 24h, single use; sign in before joining | Families share through WhatsApp; a leaked link expires quickly (see ADR 0006) |
| 38 | Only the owner invites and removes members; removal is a `DELETE` on `household_members` | Simple access control for health data; no extra table (see ADR 0006) |
| 39 | Low stock: optional `medications.minimum_quantity`; low when active batches sum below it | One threshold cannot fit tablets and syrups; opt-in avoids noise (see ADR 0007) |
| 40 | Alert frequency per user and household: `off` / `daily` (default) / `weekly`; daily cron sends only when due and relevant | Users control the noise; rules live in the API, testable (see ADR 0008) |
| 41 | Tables `fcm_device_tokens` (several devices per user) and `notification_preferences` | Push needs device tokens; preferences need `last_sent_at` (see ADR 0008) |
| 42 | Stable `main`, short `feature/` and `fix/` branches, merge only via PR after CI | CI protects `main`; PRs show working habits to recruiters (see ADR 0009) |
| 43 | Conventional Commits | Readable history, easy changelog (see ADR 0009) |
| 44 | MIT license | Permissive and short; lets anyone reuse the code (see ADR 0009) |
| 45 | Calendar dates (`expiration_date`, `opened_at`) as `LocalDate` / `DATE`; month/year expiry stored as the last day of the month | Expiry has no time of day; many boxes print only month/year (see ADR 0010) |
| 46 | Moments in time as `Instant` / `TIMESTAMPTZ` (UTC), never `TIMESTAMP` without zone | Unambiguous storage; convert only for display (see ADR 0010) |
| 47 | "Today" in fixed `America/Sao_Paulo` (config `apotheca.time-zone`) via an injected `Clock` bean; status computed only in the API | Server runs in UTC; testable with `Clock.fixed`; screen and push agree (see ADR 0010) |
| 48 | Primary keys `BIGINT` identity; every request checks household membership | Simple and readable; security comes from access checks, not secret ids (see ADR 0011) |
| 49 | Medications and storage locations belong to one household, no global catalog | No household sees another's data; ANVISA autocomplete can solve repetition later (see ADR 0011) |
| 50 | Hard delete with `ON DELETE CASCADE`; storage location `RESTRICT` while it holds batches | No forgotten `deleted_at` filters; real erasure fits LGPD (see ADR 0011) |
| 51 | Stock movements typed (`INITIAL`/`USE`/`DISCARD`/`ADJUSTMENT`) with signed change; batch creation writes `INITIAL` | History always sums to `current_quantity`, a simple test invariant (see ADR 0011) |
| 52 | Quantities `NUMERIC(10,2)` / `BigDecimal` | 2.5 ml of syrup, half tablets (see ADR 0011) |
| 53 | Enums as uppercase `VARCHAR` + `CHECK`, `@Enumerated(STRING)` | Native PG enums are awkward to change in migrations (see ADR 0011) |
| 54 | API as OAuth2 resource server validating Firebase ID tokens (signature, expiry, issuer, audience = project ID) | No passwords in the project; configuration instead of the Firebase Admin SDK (see ADR 0012) |
| 55 | Every endpoint requires a token; only `/health` and Swagger are public | New endpoints are protected without anyone remembering (see ADR 0012) |
| 56 | Stateless API, CSRF disabled, CORS allow-list in `apotheca.cors.allowed-origins` | Bearer tokens are not sent automatically like cookies; free instances lose sessions (see ADR 0012) |
| 57 | `users` row created on the first `GET /api/me` (get or create) | Explicit and testable, unlike a filter on every request (see ADR 0012) |
| 58 | Sign-in with Google only in the MVP | One tap, no passwords; email/password can be added without API changes (see ADR 0012) |
| 59 | Firebase JS SDK wrapped in our own `AuthService`, no AngularFire | AngularFire tends to lag behind new Angular versions (see ADR 0012) |
| 60 | Bearer scheme in the OpenAPI document (Swagger "Authorize" button) | Protected endpoints stay testable from the browser (see ADR 0012) |
| 61 | Fonts self-hosted through `@fontsource` npm packages, not the Google Fonts CDN | Works offline in the PWA; no third-party requests with users' IPs (see ADR 0005) |

## Commands

### Backend (run inside `backend/`)

No local Maven install is needed: `./mvnw` downloads the right Maven version.

| Command | What it does |
|---|---|
| `./mvnw spring-boot:run` | Start the API on http://localhost:8080 |
| `./mvnw verify` | Compile, run all tests and build the jar (same as CI); needs Docker running (Testcontainers) |
| `./mvnw test -Dtest=HealthControllerTest` | Run a single test class |
| `curl localhost:8080/health` | Check that the API is up (`{"status":"ok"}`) |

Swagger UI: http://localhost:8080/swagger-ui.html (OpenAPI JSON at `/v3/api-docs`).

### Frontend (run inside `frontend/`)

Use the project's Angular CLI (`npx ng` or the npm scripts), not a global `ng`.

| Command | What it does |
|---|---|
| `npm install` | Install dependencies (CI uses `npm ci`) |
| `npm start` | Start the app on http://localhost:4200 |
| `npx ng lint` | Run ESLint (same as CI) |
| `npx ng test --watch=false` | Run all unit tests once with Vitest (same as CI) |
| `npx ng test --watch=false --include src/app/features/home/home.spec.ts` | Run a single test file |
| `npx ng build` | Production build into `dist/apotheca` (same as CI) |

### Local database (Docker Compose, root of the repo)

| Command | What it does |
|---|---|
| `docker compose up -d` | Start PostgreSQL 17 in the background |
| `docker compose ps` | Show container state and healthcheck |
| `docker compose logs -f db` | Follow the database logs |
| `docker compose exec db psql -U apotheca -d apotheca` | Open a psql shell |
| `docker compose down` | Stop the container, keep the data |
| `docker compose down -v` | Stop **and delete** the volume (fresh database) |

Local connection string (defaults, overridable through `POSTGRES_*` environment variables):
`jdbc:postgresql://localhost:5432/apotheca`, user `apotheca`, password `apotheca`.
Development credentials only — production runs on Neon with platform secrets.
