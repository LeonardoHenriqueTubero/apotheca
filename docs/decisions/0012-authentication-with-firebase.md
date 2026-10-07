# ADR 0012: Authentication with Firebase

- **Status:** Accepted
- **Date:** 2026-10-03
- **Decision log:** #54, #55, #56, #57, #58, #59, #60, #63, #64

## Context

The stack already fixes Firebase Authentication for sign-in and Spring Security on the
API (decision #4). What remained open was how the two connect: how the API checks a
token, which endpoints are public, how a Firebase account becomes a row in `users`,
which sign-in methods the family gets, and how the Angular app talks to Firebase.

The API never sees passwords. Firebase signs the user in and issues an **ID token**
(a JWT signed by Google, valid for one hour); the app sends it on every request as
`Authorization: Bearer <token>`.

## Decision

### 1. The API is an OAuth2 resource server

`spring-boot-starter-oauth2-resource-server` validates every token: Google's signature
(public keys from the `securetoken` JWK set), expiry, issuer
`https://securetoken.google.com/<projectId>` and audience `<projectId>`. The project ID
is the configuration property `apotheca.firebase.project-id`; production overrides it
with `APOTHECA_FIREBASE_PROJECT_ID`. Development uses the separate `apotheca-dev-e2f60`
Firebase project (non-negotiable rule 2).

### 2. Protected by default

Every endpoint requires a valid token. Only `/health` (used to wake the server on
Render) and the Swagger UI / OpenAPI documents are public. A new endpoint is protected
without anyone having to remember it.

### 3. Stateless, no CSRF, explicit CORS

- No HTTP session: each request proves itself with its token, which also survives the
  free instance being stopped.
- CSRF protection is disabled. CSRF abuses cookies that the browser sends
  automatically; this API does not use cookies for authentication, and a malicious
  site cannot add the `Authorization` header.
- CORS allows only the origins in `apotheca.cors.allowed-origins`
  (`http://localhost:4200` in development; the Firebase Hosting domain is added on deploy).

### 4. `users` rows are created on `GET /api/me`

The app calls `GET /api/me` right after sign-in. If no user has the token's UID
(`sub`), one is created with the token's `email` and `name`. `UserService` receives
plain values, not the `Jwt`, so it does not depend on Spring Security. The response is
a DTO without the Firebase UID.

### 5. Sign-in with Google only (MVP)

One tap, no password to forget, and almost every Android phone already has a Google
account. Email/password can be enabled later in Firebase without any API change, since
the token is the same.

### 6. Firebase JS SDK, wrapped in our own service

The Angular app uses the official `firebase` package directly, wrapped in an
`AuthService` with signals (ADR 0002), plus a guard and an HTTP interceptor in `core/`.
No AngularFire.

- Sign-in opens a **popup**. Redirect sign-in is unreliable while the app and the Firebase
  auth domain are on different sites; it is re-evaluated on the first deploy, testing the
  installed PWA on real phones.
- The interceptor sends the token only to URLs under `apiUrl`.
- The Firebase API key is not a secret, but it is **restricted** in Google Cloud: only the
  app's sites (`http://localhost:4200`, `https://<projectId>.firebaseapp.com`) and only the
  Identity Toolkit and Token Service APIs.

### 7. Bearer authentication in Swagger UI

The OpenAPI document declares a bearer scheme, so Swagger UI shows an **Authorize**
button and a lock on protected endpoints. `/health` is marked as public.

## Alternatives considered

- **Firebase Admin SDK on the backend.** Also validates tokens, but adds a large
  dependency and service-account credentials; Spring Security does the same check with
  configuration only.
- **Creating the user in a filter on every request.** Works, but is implicit and runs on
  every call; one explicit endpoint is easier to follow and to test.
- **Google plus email/password from the start.** Nobody is left out, but needs sign-up,
  password reset and email verification screens. Can be added later.
- **AngularFire.** Angular-friendly wrappers, but it often lags behind new Angular
  versions; the project is on Angular 22.
- **Session cookies.** Would need CSRF protection and server-side state that is lost
  when the free instance stops.

## Consequences

- ✅ No passwords stored or handled by the project.
- ✅ Security is tested without Firebase: tests use mock JWTs from `spring-security-test`.
- ✅ New endpoints are protected by default.
- ⚠️ A user's email and name are copied once, on creation; later changes in the Google
  account are not reflected yet.
- ⚠️ Two simultaneous first requests can race; the unique `firebase_uid` prevents a
  duplicate and one request fails. Acceptable for the MVP.
- ⚠️ People without a Google account cannot sign in until another method is enabled.

## Update (2026-10-06, first deploy)

- **Popup kept.** Sign-in with the popup works on the deployed app
  (`https://apotheca-48f83.web.app`) on two Android phones (Chrome and Brave). Redirect is
  not needed for now. Still to check: Safari on iOS, and the installed PWA once it exists.
- **Two Firebase projects, two API keys.** The development project (`apotheca-dev-e2f60`)
  allows only `http://localhost:4200`; the family project (`apotheca-48f83`) allows only
  `https://apotheca-48f83.web.app/*` and `https://apotheca-48f83.firebaseapp.com/*`.
  Both are limited to the Identity Toolkit and Token Service APIs.
