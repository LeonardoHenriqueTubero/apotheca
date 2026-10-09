# ADR 0015: Batch status and stock movements

- **Status:** Accepted
- **Date:** 2026-10-09
- **Decision log:** #85, #86, #87, #88, #89, #90

## Context

Roadmap part 6.4 adds the batches API: one bought box of a medication, with its expiry,
quantity and storage location. ADR 0011 already fixed the storage side: every change of
quantity is a typed `stock_movements` row (`INITIAL`, `USE`, `DISCARD`, `ADJUSTMENT`) and
`sum(quantity_change) = current_quantity`. ADR 0010 fixed how "today" is computed and
ADR 0007 which batches count as stock.

Still open were the rules for what each action does, how a box that is used up is shown,
and how the opening date is filled in, since the effective expiry of syrups and eye drops
depends on it.

## Decision

### 1. Quantity changes only through movements

`PUT .../batches/{id}` edits the storage location, the printed expiry and the opening date,
never the quantity. Quantity changes through three actions, each writing one movement in the
same transaction:

| Endpoint | Body | Movement | Rule |
|---|---|---|---|
| `POST .../batches/{id}/use` | `{"quantity": 5}` | `USE`, −5 | 409 if it exceeds what is left |
| `POST .../batches/{id}/discard` | none | `DISCARD`, −(all that is left) | 409 if already empty |
| `POST .../batches/{id}/adjust` | `{"quantity": 80}` (counted) | `ADJUSTMENT`, the difference | no movement if nothing changed |

`GET .../batches/{id}/movements` lists the history, newest first. Creating a batch writes
`INITIAL`. Deleting a batch removes its history too and is meant for registration mistakes;
throwing a box away is a discard, which keeps the history.

### 2. The first use opens the box

When a box without `opened_at` is used, `opened_at` becomes today (São Paulo time, ADR 0010).
Its shelf life after opening starts without anyone remembering to fill in the date; a wrong
date can be fixed by editing the batch.

### 3. Status `EMPTY` comes before the dates

`BatchStatus` gains `EMPTY`. A batch with `current_quantity = 0` is `EMPTY` even if its date
has passed; otherwise the status is `EXPIRED`, `EXPIRING_SOON` or `OK` as before. A box that
was used up is not "expired medicine in the house", so summaries and alerts ignore it,
consistent with ADR 0007, where empty batches are not active stock. On screen it carries no
status color, since it asks for no action (ADR 0005).

### 4. The opening date cannot be in the future, in São Paulo time

The check runs in `BatchService` with the injected `Clock`, not with `@PastOrPresent`, which
uses the server clock in UTC and would accept tomorrow's date from 21:00 to midnight in
Brazil. The error keeps the validation shape: `400` with `"errors": {"openedAt": ...}`.

### 5. Movements lock the batch row

Use, discard and adjust load the batch with `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`).
Two phones using the same box at once are served one after the other, so neither reads a
stale quantity and the history still adds up.

## Alternatives considered

- **Editable quantity in `PUT`.** Simpler form, but the history would stop adding up.
- **One generic `POST .../movements` with a type.** Fewer endpoints, but `quantity` would mean
  "amount used" for one type and "amount counted" for another.
- **Discard a given quantity.** More flexible, but throwing away an expired box would need
  counting and typing; partial losses are covered by an adjustment.
- **Keep date-only status and filter empty boxes in each consumer.** Every summary, screen
  and alert would have to remember the filter.
- **Optimistic locking with a `@Version` column.** Needs a migration and retry handling;
  a short row lock is enough for a family app.

## Consequences

- ✅ The movement history always adds up to the current quantity, and tests check it.
- ✅ Syrups and eye drops get the right effective expiry without extra steps.
- ✅ Used-up boxes never raise "expired" warnings.
- ⚠️ An automatic opening date can be off by a few days if a use is registered late.
- ⚠️ The screens must make "delete" (mistake, history lost) clearly different from
  "discard" (thrown away, history kept).
