# ADR 0013: API deployment on Render

- **Status:** Accepted
- **Date:** 2026-10-06
- **Decision log:** #66, #67, #68, #69

## Context

Render (API) and Neon (database) were chosen in the decision log (#6, #7): free plans,
no card, and Docker from day one so the same image can move to Cloud Run later.
Before the first deploy we need to fix *how* the API is packaged and configured:

- Render's free plan gives the container **512 MB** of RAM and sets the port through a
  `PORT` environment variable.
- The repository is a monorepo: a frontend change should not redeploy the API.
- The infrastructure must live in Git (CLAUDE.md, rule 2), and secrets must not (rule 3).
- Render has no region in South America. Neon offers AWS regions, including `us-east-1`
  (N. Virginia).

## Decision

### 1. Multi-stage Dockerfile in `backend/`

A JDK stage runs `./mvnw package -DskipTests`; a JRE stage copies only the jar and runs it
as an unprivileged user. Build files are copied before the source, so the dependency
download is cached between builds. Tests are not run in the image: they need Docker
(Testcontainers) and already run in CI.

The JVM sizes its heap from the container limit (`-XX:MaxRAMPercentage=75`), and the
API reads its port from `server.port=${PORT:8080}`.

### 2. Render Blueprint (`render.yaml`) at the repository root

One free Docker web service, `apotheca-api`, with:

- `rootDir: backend`: only changes under `backend/` trigger a deploy.
- `autoDeployTrigger: checksPass`: a commit on `main` is deployed only after
  `backend-ci.yml` passes, so CI is the gate to production.
- `healthCheckPath: /health`: Render switches traffic to a new version only when it
  answers, so a broken release (wrong database password, failed migration) never
  replaces a working one.

### 3. Region: Virginia, next to Neon `us-east-1`

A single API request can run several queries, so the distance between API and database
matters more than the distance between the phone and the API. Both run in N. Virginia;
users in Brazil pay one round trip to the US per request, not one per query.

The API uses Neon's **direct** connection, not the pooled (`-pooler`, PgBouncer) one:
Flyway migrations are safer on a direct session, and a family app needs few connections.

### 4. Configuration through environment variables

`render.yaml` lists the variables with `sync: false`; their values are typed once in the
Render dashboard. Spring Boot's relaxed binding maps them onto the existing properties:

| Variable | Property |
|---|---|
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | `spring.datasource.*` (Neon) |
| `APOTHECA_FIREBASE_PROJECTID` | `apotheca.firebase.project-id` (family project) |
| `APOTHECA_CORS_ALLOWEDORIGINS` | `apotheca.cors.allowed-origins` (Hosting domains) |

`application.properties` keeps the local defaults (Docker Compose, development Firebase
project), which production always overrides.

## Alternatives considered

- **Render's native runtime instead of Docker.** No Dockerfile to maintain, but ties the
  build to Render and drops the "same image on Cloud Run" goal (#7).
- **Single-stage image with the JDK.** Simpler, but larger and ships the compiler and
  build tools to production.
- **Run the tests inside `docker build`.** Would need Docker inside Docker for
  Testcontainers; CI already runs them.
- **Configure the service by hand in the dashboard.** Faster once, but nothing is versioned
  or reviewable.
- **Oregon or Ohio.** No advantage for users in Brazil, and farther from Neon `us-east-1`.
- **Neon pooled connection.** Useful with many short-lived connections (serverless);
  not needed here, and session features used by Flyway can misbehave behind it.

## Consequences

- ✅ The same image runs locally (`docker build`/`docker run`) and on Render.
- ✅ No deploy without green CI; no traffic to a version that fails its health check.
- ✅ Infrastructure changes go through pull requests like code.
- ⚠️ Variables with `sync: false` are only asked for when the Blueprint is created; later
  changes to their values are made in the dashboard, not in `render.yaml`.
- ⚠️ The free instance sleeps when idle; the first request takes about a minute
  (the UI's "waking up the server" state).
- ⚠️ Every request crosses from Brazil to the US (roughly 120–150 ms). Acceptable for an
  inventory app; revisit if the API moves to Cloud Run (`southamerica-east1`).
