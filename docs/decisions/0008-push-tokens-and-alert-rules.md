# ADR 0008: Push device tokens and alert rules

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #40, #41

## Context

Alerts are sent as push notifications through Firebase Cloud Messaging (FCM),
triggered by a GitHub Actions cron that calls a protected endpoint (decision #8).
To send a push, the API needs the FCM token of each device, and it needs rules
for **when** and **how often** to send.

Too many notifications make people disable them; too few make the app useless.
A user may belong to several households and care about them differently.

## Decision

### 1. Frequency chosen by the user, per household

Each user chooses, for each household, one of:

| Value | Meaning |
|---|---|
| `off` | never send alerts for this household |
| `daily` | at most one alert per day (**default**) |
| `weekly` | at most one alert every 7 days |

### 2. Cron runs daily, sends only when it is "time" and there is news

The GitHub Actions cron runs **once a day** and calls the protected alert endpoint.
For each preference, the API:

1. skips it if `frequency = off`;
2. skips it if the last alert is too recent (`last_sent_at` less than 1 day ago for
   `daily`, less than 7 days ago for `weekly`);
3. checks the household for something relevant: expired batches, batches expiring
   within 30 days, or low-stock medications (ADR 0007);
4. if there is nothing relevant, sends nothing;
5. otherwise sends one push to each of the user's devices and updates
   `last_sent_at`.

Doing the scheduling logic in the API, instead of having several cron jobs, keeps
one simple workflow and puts the rules where they can be unit tested.

### 3. New table: `fcm_device_tokens`

| Column | Type | Notes |
|---|---|---|
| `id` | PK | |
| `user_id` | FK → `users` | |
| `token` | string | the FCM registration token of the device, unique |
| `platform` | enum `web` \| `android` \| `ios` | |
| `created_at` | timestamp | |
| `last_used_at` | timestamp | updated when the app registers the token again |

A user can have several devices (phone and laptop). When FCM reports a token as
invalid, the API deletes it.

### 4. New table: `notification_preferences`

| Column | Type | Notes |
|---|---|---|
| `id` | PK | |
| `user_id` | FK → `users` | |
| `household_id` | FK → `households` | |
| `frequency` | enum `off` \| `daily` \| `weekly` | default `daily` |
| `last_sent_at` | timestamp, nullable | `NULL` until the first alert |

One row per user and household (unique on `user_id, household_id`).

## Alternatives considered

- **Fixed daily alert for everyone.** Simplest, but no way to reduce noise other
  than disabling notifications entirely in the phone settings.
- **One global preference per user.** Less flexible for someone who belongs to
  their own home and a parent's home.
- **Separate cron jobs for daily and weekly.** More workflows to maintain, and the
  rules would be split between YAML and Java.
- **`@Scheduled` inside Spring.** Rejected by decision #8: free Render instances
  scale to zero, so an in-process scheduler may never run.

## Consequences

- ✅ Users control the noise; nobody gets an empty "all good" notification.
- ✅ One daily workflow; all rules live in the API and are testable.
- ✅ Invalid tokens are cleaned up, so the table does not grow forever.
- ⚠️ On iOS, push only works when the PWA is installed on the home screen; the UI
  must explain this.
- ⚠️ "1 day" and "7 days" depend on the timezone, which is still an open decision
  (expected: `America/Sao_Paulo`).
