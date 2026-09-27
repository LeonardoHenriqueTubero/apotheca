# ADR 0009: Git workflow and license

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #42, #43, #44

## Context

Until now commits went straight to `main`. The repository is public and is a
portfolio: recruiters look at the history and at pull requests to see how the
developer works, and the CI workflows (ADR 0004) should run before code reaches
`main`.

A public repository without a license is, legally, "all rights reserved": nobody
may reuse the code, even though they can read it.

## Decision

### 1. Short-lived branches, merge only through pull requests

- `main` is always stable: CI passes and the app builds.
- Work happens on short-lived branches, named by type:
  - `feature/...` for new functionality (`feature/household-invites`)
  - `fix/...` for bug fixes (`fix/expiry-timezone`)
  - `docs/...` and `chore/...` for documentation and maintenance
- Changes reach `main` only through a **pull request**, after the CI workflows pass.
- Branches are deleted after merge.

### 2. Conventional Commits

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):
`type(optional scope): description`, for example:

```
feat(backend): add household invite endpoint
fix(frontend): show expired status on batch list
docs: add ADR 0009 for git workflow and license
```

Common types: `feat`, `fix`, `docs`, `chore`, `ci`, `test`, `refactor`.

### 3. MIT license

The code is published under the **MIT License** (`LICENSE` at the repository root).

## Alternatives considered

- **Commit directly to `main`.** Faster for a solo developer, but CI only runs
  after the fact and the history shows no review habit.
- **Git Flow (`develop`, `release/*`, `hotfix/*`).** Designed for versioned
  releases; too much ceremony for a continuously deployed web app.
- **Free-form commit messages.** Easier, but the history is harder to read and
  harder to turn into a changelog.
- **GPL or Apache 2.0.** GPL obliges derivative works to stay open, which can make
  companies wary of looking at the code; Apache 2.0 is fine but longer and adds
  patent clauses a personal project does not need.
- **No license.** Nobody could legally reuse the code.

## Consequences

- ✅ CI protects `main`; every change has a pull request that explains it.
- ✅ The history is readable and shows professional habits to recruiters.
- ✅ Anyone may use, copy and modify the code, as long as the copyright notice is kept.
- ⚠️ A solo developer opens and merges their own pull requests, which adds a few
  steps per change.
- ⚠️ To enforce "merge only through PR", `main` should be protected in the GitHub
  repository settings (branch protection requiring the CI checks).
