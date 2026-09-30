# ADR 0010: Date and time handling

- **Status:** Accepted
- **Date:** 2026-09-29
- **Decision log:** #45, #46, #47

## Context

Most of the app's value comes from dates: a batch is **expired**, **expires within
30 days** or is fine. Before writing the `V1` migration we need to fix how dates are
stored, because a migration cannot be edited once applied.

Three facts shape the decision:

- An expiry date has no time of day: "expires on 31/10/2027" holds for the whole day.
- Many Brazilian boxes print only month and year ("VAL 10/2027").
- The API runs on Render in **UTC**, while every user is in Brazil. At 22:00 on
  30/10 in São Paulo it is already 01:00 on 31/10 in UTC, so a server using its
  own clock would mark a batch expiring on 30/10 as expired three hours early.

## Decision

### 1. Calendar dates: `LocalDate` / `DATE`

`batches.expiration_date` and `batches.opened_at` are `DATE` columns, mapped to
`java.time.LocalDate`. Nobody knows the hour a syrup was opened, and expiry is per day.

When the user types only month/year, the app stores the **last day of that month**
(`10/2027` → `2027-10-31`), the usual reading of a month/year expiry. The UI accepts
both formats; the database always holds a full date, so the effective expiry rule
(the earlier of the printed expiry and `opened_at + shelf_life_after_opening_days`)
stays a plain date comparison.

### 2. Moments in time: `Instant` / `TIMESTAMPTZ`

Columns that record *when something happened* (`created_at`, stock movement time,
`notification_preferences.last_sent_at`, `fcm_device_tokens.last_used_at`) are
`TIMESTAMPTZ`, mapped to `java.time.Instant`. They are always UTC in storage and are
converted to local time only for display. `TIMESTAMP` without time zone is not used.

### 3. "Today" is São Paulo time, through an injected `Clock`

Business rules compute "today" in a fixed zone, `America/Sao_Paulo`, set by a
configuration property (`apotheca.time-zone`). The zone is exposed as a Spring
`Clock` bean, and code calls `LocalDate.now(clock)`, never `LocalDate.now()`:

```java
@Bean
Clock clock(@Value("${apotheca.time-zone}") String zone) {
    return Clock.system(ZoneId.of(zone));
}
```

Tests replace it with `Clock.fixed(...)`, so rules such as "expired" or "weekly
alert is due" can be tested for any date.

Status is computed only in the API, so the list screen and the push always agree.

### 4. Related conventions

- **JSON:** ISO 8601. Dates as `"2027-10-31"`, instants as `"2026-09-29T14:30:00Z"`.
- **Frontend:** expiry dates are handled as `yyyy-MM-dd` values (or through the
  Angular Material date adapter), never with `new Date("2027-10-31")`, which parses
  as UTC midnight and shows the previous day in Brazil.
- **Alert cron:** GitHub Actions cron runs in UTC; `0 11 * * *` means 08:00 in
  Brasília. Brazil has had no daylight saving time since 2019, so this stays fixed.

## Alternatives considered

- **Always require a full expiry date.** Simpler form, but makes users invent a day
  that is not on the box.
- **Store expiry as `YearMonth`.** Faithful to the box, but the effective expiry
  compares it with a full date (`opened_at` + days), which gets awkward.
- **`TIMESTAMP` without time zone.** Ambiguous: a stored `23:00` could be UTC or
  local time, and nobody can tell later.
- **Time zone per user.** Correct for a global app, but the users are in Brazil and
  the extra column and logic are not needed in the MVP.
- **Status computed by the frontend with the phone clock.** Duplicates the rule, and
  the server-side push could disagree with the screen.

## Consequences

- ✅ No ambiguity in stored data; the schema is ready for `V1`.
- ✅ Date rules are deterministic in tests thanks to the injected `Clock`.
- ✅ Screen and push notifications use the same "today".
- ⚠️ Users in other Brazilian zones (for example Manaus, one hour behind) see the day
  change one hour early. Accepted for the MVP; a per-user zone can be added later.
- ⚠️ The UI must support entering either a full date or month/year for expiry.
