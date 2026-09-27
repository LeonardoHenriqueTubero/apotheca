# ADR 0006: Household invite flow

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #37, #38

## Context

A household (family, a friend's home) owns medicines, and users can belong to
several households. We need a way for someone to join a household, and a way to
remove a member.

Constraints:

- Most users are family and friends who will receive the invite through WhatsApp or
  similar apps, on a phone.
- The invited person may not have an Apotheca account yet.
- Household data is health-related (LGPD), so access must be controlled and an
  invite must not stay valid forever.

## Decision

### 1. Shareable link, created by the owner

The owner generates an invite in the app and shares it as a link. The app uses the
browser's native share sheet (`navigator.share()`), which opens WhatsApp, Telegram,
e-mail and so on. Where the Web Share API is not available (some desktop browsers),
the app falls back to copying the link to the clipboard.

The link carries a **random, unique token**:

- expires **24 hours** after it was created;
- can be used **only once**;
- adds the user to the household with role `member`.

### 2. Sign in before joining

If the invited person is not signed in, the app asks them to sign in or create an
account with Firebase Authentication first, then returns to the invite and joins
the household. The token is only consumed after sign-in.

### 3. Only the owner manages members

Only users with role `owner` in the household can create invites and remove
members. Removing a member is a `DELETE` on `household_members`; no extra table
is needed.

### 4. New table: `household_invites`

| Column | Type | Notes |
|---|---|---|
| `id` | PK | |
| `household_id` | FK → `households` | |
| `token` | string | unique, indexed; generated with a cryptographically secure random generator |
| `created_by` | FK → `users` | the owner who created it |
| `expires_at` | timestamp | `created_at + 24h` |
| `used_at` | timestamp, nullable | set when the invite is accepted |
| `used_by` | FK → `users`, nullable | who accepted it |

An invite is valid when `used_at IS NULL` and `expires_at` is in the future.
Keeping used and expired invites (instead of deleting them) gives a simple history
of who joined and through which invite.

## Alternatives considered

- **Invite by e-mail.** Needs an e-mail sending service and the person's address,
  and families mostly talk through WhatsApp.
- **Permanent household code.** Simpler, but anyone who ever saw the code could
  join forever. Too weak for health data.
- **Join request approved by the owner.** Safer, but adds a screen and a waiting
  step the MVP does not need; the short, single-use token already limits abuse.

## Consequences

- ✅ Natural on phones: sharing a link is how people already send things.
- ✅ A leaked link is useless after 24 hours or after it is used once.
- ✅ Works for people who do not have an account yet.
- ⚠️ If the owner shares the link in a group chat, whoever opens it first joins.
  The owner can remove that member afterwards.
- ⚠️ Rules not covered here (for example, whether the last owner can leave, or
  transferring ownership) are decided when the members feature is implemented.
