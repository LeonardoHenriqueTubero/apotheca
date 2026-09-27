# ADR 0004: Continuous integration and frontend linting

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #13, #31, #32

## Context

The backend already has a GitHub Actions workflow (`backend-ci.yml`) that runs
`./mvnw verify` on every push and pull request to `main`. The frontend is about to
be created, and the Stack only mentioned backend CI, so it was not clear whether
the frontend would have CI, in which workflow, and with which steps.

For a portfolio project, CI is proof that the code builds and the tests pass,
shown to recruiters as a badge in the README.

## Decision

### 1. One workflow per app, filtered by path

| Workflow | Runs when these paths change | Steps |
|---|---|---|
| `backend-ci.yml` | `backend/**`, the workflow file | `./mvnw -B verify` |
| `frontend-ci.yml` | `frontend/**`, the workflow file | `npm ci` → `ng lint` → `ng test --watch=false` → `ng build` |

Both run on push and pull request to `main`, can be started by hand
(`workflow_dispatch`), cancel older runs of the same branch, and have their own
badge in the README.

The frontend steps go from the fastest to the slowest, so a lint error fails the
run in seconds. Tests run once and exit (`--watch=false`). Vitest runs in Node
with a simulated DOM, so no browser is installed in CI.

### 2. ESLint on the frontend, default configuration

ESLint is added with `ng add angular-eslint`, which is the setup recommended by
the Angular CLI. The rules are the defaults it generates; no rule is customized
for now. `ng lint` is a CI step, so code that breaks a rule does not reach `main`.

## Alternatives considered

- **A single workflow for the whole repo.** Simpler to read, but a CSS change
  would run the Maven build and a Java change would run the Angular build.
  Separate workflows are faster and show which side is broken.
- **No lint in CI** (only in the editor). It depends on each developer's setup;
  CI enforces the same rules for everyone.
- **Custom ESLint rules or Prettier from day one.** It is possible, but there is
  no real problem to solve yet. Rules will be added when a concrete need appears.

## Consequences

- ✅ Each side of the monorepo has its own fast pipeline and its own badge.
- ✅ Lint, tests and a production build are checked on every change to the frontend.
- ⚠️ A change that touches both `backend/` and `frontend/` runs both workflows.
  This is expected.
- ⚠️ Path filters mean a change outside `backend/` and `frontend/` (for example
  docs only) runs no CI. This is intended: there is nothing to build.
