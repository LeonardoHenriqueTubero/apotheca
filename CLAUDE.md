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
  (what it is, screenshot, stack, how to run, demo link). Details go in ADRs (English only).
- UI text: **pt-BR**.

## Stack (decided)

| Area | Choice |
|---|---|
| Frontend | Angular (recent version, standalone components), installable **PWA** |
| Backend | Java 21, Spring Boot (latest stable at project creation, verify), **Maven** |
| Database | PostgreSQL (**Neon** free plan in production, Docker Compose locally) |
| Migrations | **Flyway** |
| Auth | **Firebase Authentication**; Angular sends the JWT, Spring Security validates it (resource server) |
| Push notifications | **Firebase Cloud Messaging** (on iOS only works when the PWA is installed) |
| Hosting | Frontend: Firebase Hosting. API: Docker container on **Render** free plan. |
| Containers | **Docker from day one** (same image must run on Render and, later, Cloud Run) |
| Tests | JUnit + **Testcontainers** (real PostgreSQL, no H2) |
| CI | **GitHub Actions** (`mvn verify` on every push/PR) |
| Scheduled alerts | GitHub Actions **cron** calling a protected endpoint (not `@Scheduled`) |
| API documentation | **springdoc-openapi** (Swagger UI), generated from the Spring Boot code |

Future option (not now): migrate the API to Google Cloud Run, only if a billing account is available.
Record it as an ADR when it happens ("why we moved from Render").

## Repository layout

```
/backend            Spring Boot API
/frontend           Angular app
/docs/decisions     ADRs (one file per decision, English)
/docs/diagrams      Mermaid diagrams (architecture, ER, alert flow)
/.github/workflows  CI and scheduled jobs
docker-compose.yml  Local PostgreSQL
CLAUDE.md
README.md / README.pt-BR.md
```

Monorepo on purpose: one PR can change API and UI together, one history, one place for docs.

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

## Domain model (draft, refine in V1 migration)

- `users` (identified by Firebase UID)
- `households`: the unit that owns medicines (family, friend's home). Users may belong to several.
- `household_members`: user + household + role (`owner` | `member`)
- `storage_locations`: "bathroom cabinet", "bag"
- `medications`: name, active ingredient, strength, form (tablet, syrup...), unit,
  optional `shelf_life_after_opening_days`
- `batches`: one bought box. Expiration date, current quantity, location,
  optional `opened_at`
- `stock_movements`: history of use / discard / adjustment per batch (do not just overwrite quantity)

Key business rule: **effective expiry = the earlier of the printed expiry and
`opened_at + shelf_life_after_opening_days`** (syrups, eye drops, reconstituted antibiotics).
Pure logic, cover with unit tests. The app only stores what the user types from the leaflet.

Medication vs batch is deliberate: two boxes of the same medicine can have different expiries.

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
- **Ideas**: barcode scanning, autocomplete from ANVISA open data, shopping list,
  guidance on correct disposal of expired medicines

## UX and design notes

- Mobile first; the app is used on phones.
- Status (expired / expiring / ok) must never rely on color alone: also use icon and text.
- Render free plan cold start is ~1 minute: the UI should show a "waking up the server" state.
- Design work happens in Claude's Design canvas (claude.ai); the result is translated into an
  Angular theme (tokens as CSS variables / Angular Material theme). Record the identity as an ADR.

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

- Visual identity for Apotheca (personality not chosen yet: options discussed were calm/trustworthy, friendly/colorful, minimalist/modern)
- UI library and Angular structure (Angular Material was only suggested)
- Household invite flow (how someone joins a household)
- Definition of "low stock" (likely a per-medication minimum quantity)
- Storage of FCM device tokens (needs a table) and alert rules (when, how often, configurable?)
- Backend package structure / architecture style
- Generated Angular client from the OpenAPI contract (Swagger UI itself is decided, see Stack; the generated client is still open)
- Date/timezone handling (expiry as `LocalDate`, alerts in America/Sao_Paulo)
- Git workflow (branches, PRs, Conventional Commits) and repository license
- Demo mode or demo account so recruiters can try the app
- Confirm Firebase Hosting works without a card when we reach the first deploy

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
| 13 | GitHub Actions CI | Automatic build/test, "build passing" badge |
| 14 | Flyway, schema only by migration files | All databases stay identical; history is in Git |
| 15 | Households + medication/batch split + stock movements | Multi-home support, different expiries per box, usage history |
| 16 | Opening-shelf-life support | Real cases: syrups, eye drops, reconstituted antibiotics |
| 17 | Price in v1.1, reviews in v2 | Keep the MVP lean; reviews are subjective and touch sensitive data |
| 18 | MCP: read/diagnose only, changes through Git | Avoids schema drift and accidental production changes |
| 19 | Three Mermaid diagrams (architecture, ER, alert flow) | Recruiters read the README first; text diagrams stay versioned and updatable |
| 20 | springdoc-openapi (Swagger UI) in the backend | Free, generated from code, lets endpoints be tested from the browser before the frontend exists; good portfolio signal |
| 21 | App name: Apotheca | Short, memorable, evokes the traditional home medicine cabinet; replaces the working title |

## Commands

Fill in the backend and frontend rows once those skeletons exist (build, test, run, migrate, lint).

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
