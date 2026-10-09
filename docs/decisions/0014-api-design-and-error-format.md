# ADR 0014: API design, error format and frontend API client

- **Status:** Accepted
- **Date:** 2026-10-06
- **Decision log:** #71, #72, #73, #74, #75, #76, #77, #78

## Context

Roadmap step 6 adds the first domain endpoints (households, storage locations, medications,
batches). Before writing them we need conventions that every endpoint follows, so the API
stays predictable for the Angular app and for anyone reading the code:

- Almost every resource belongs to a household, and every request must check that the user
  is a member of it (ADR 0011, #48).
- Errors must reach the frontend in one shape, whatever their cause.
- The open decision "generated Angular client from the OpenAPI contract" must be closed
  before the first frontend service that talks to these endpoints.

## Decision

### 1. URLs nested under the household

Household-scoped resources live under their household:

```
POST /api/households                              create (creator becomes OWNER)
GET  /api/households                              the user's households
GET  /api/households/{householdId}                one household
GET  /api/households/{householdId}/medications    (step 6.3)
```

Plural nouns, standard HTTP methods. `POST` answers `201 Created` with a `Location` header
and the created resource. The household in the path makes the access check explicit: the
controller always has the household ID to check.

### 2. One access check, and 404 for non-members

`HouseholdService.requireMembership(householdId, user)` is the single method every
household-scoped request goes through. When the household does not exist **or** the user is
not a member, it throws `NotFoundException`, which answers **404**. A user cannot tell
"exists but not mine" from "does not exist", so IDs (simple `BIGINT`s, #48) reveal nothing.

The current user comes from the validated token through `UserService.getOrCreate(Jwt)`,
which also creates the `users` row if the first request is not `GET /api/me`.

### 3. Errors as RFC 9457 problem details

A `@RestControllerAdvice` (`GlobalExceptionHandler`, extending Spring's
`ResponseEntityExceptionHandler`) answers every error as `application/problem+json`:

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400,
  "detail": "Invalid request content.", "instance": "/api/households",
  "errors": { "name": "must not be blank" } }
```

Validation uses Bean Validation annotations on the request records (`@NotBlank`, `@Size`)
and `@Valid` in the controller; invalid fields are listed in `errors`. Messages are English
and technical: the frontend shows its own pt-BR messages and uses the API's only as a fallback.

| Status | When |
|---|---|
| 400 Bad Request | invalid request body (listed in `errors`) |
| 404 Not Found | the resource does not exist, or belongs to a household the user is not in |
| 409 Conflict | a valid request that clashes with the data: a duplicate name, or deleting a storage location that still holds batches (`ConflictException`) |

The 409 checks run in the service before writing, so the user gets a clear message; the
database constraints (`UNIQUE`, `ON DELETE RESTRICT`) stay as the last safety net.

### 3a. Names inside a household

Names that must be unique in a household (storage locations, step 6.2) are compared **ignoring
case**: "Bolsa" and "bolsa" are the same place for a family. Lists are sorted **in Java with a
pt-BR `Collator`**, not with SQL `ORDER BY`: the Alpine PostgreSQL image sorts by byte value
("Z" before "a", accented letters last), and Neon may sort differently. The frontend keeps the
same order with `localeCompare('pt-BR')`.

### 4. Frontend: hand-written services

Each feature has a service using `HttpClient`, and `shared/models/` holds TypeScript
interfaces that mirror the API's DTOs (for example `Household` ↔ `HouseholdResponse`).
No code is generated from the OpenAPI document; Swagger UI stays the API's documentation.

### 5. The current household is app-wide state in `core/`

`HouseholdService` (signals) and `householdGuard` live in `core/household/`, because every
feature works inside the current household and features must not import each other (ADR 0002).
The guard sends a user without a household to `/households/new`. The MVP shows the first
household; switching between households comes with invites (step 6b).

Angular runs a route's guards **at the same time**, so `householdGuard` first waits for
Firebase to restore the session (`AuthService.isSignedIn()`); otherwise, on a page reload, its
request leaves without a token and gets a 401.

## Alternatives considered

- **Flat URLs with a query parameter** (`/api/medications?householdId=1`). Easy to forget
  the parameter or the check; the nested path makes both visible.
- **403 Forbidden for non-members.** More precise, but confirms that the household exists.
- **A custom error JSON.** Would need its own documentation; RFC 9457 is a standard that
  Spring supports out of the box.
- **Generated client (openapi-generator).** Keeps types in sync automatically, but adds a
  generator to the build and code that is hard to read. The API is small, and hand-written
  services are easier to explain and to test.
- **Household state inside a feature folder.** Would force other features to import it,
  breaking ADR 0002.

## Consequences

- ✅ Every household-scoped endpoint has the same shape and the same access check.
- ✅ One error format for the frontend to handle; validation errors name the field.
- ✅ No build-time code generation; each service is plain, testable Angular code.
- ⚠️ TypeScript models must be updated by hand when a DTO changes (in the same PR).
- ⚠️ A household-scoped controller that forgets `requireMembership` leaks data; each one
  gets a test where another user receives 404.
