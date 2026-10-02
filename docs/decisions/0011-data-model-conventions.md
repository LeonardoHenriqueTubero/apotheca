# ADR 0011: Data model conventions

- **Status:** Accepted
- **Date:** 2026-09-29
- **Decision log:** #48, #49, #50, #51, #52, #53

## Context

The domain model in `CLAUDE.md` lists the tables but leaves open choices that change
every table: the type of the primary keys, whether medications are shared between
households, what happens on deletion, how stock movements are written, and how
quantities and enums are stored. They must be fixed before drawing the
[ER diagram](../diagrams/er.md) and writing the `V1` migration.

## Decision

### 1. Primary keys: `BIGINT` identity

Every table has `id BIGINT GENERATED ALWAYS AS IDENTITY`, mapped in JPA with
`@GeneratedValue(strategy = GenerationType.IDENTITY)` (as in ADR 0001).

Ids are guessable (`/medications/42`), so **every request checks that the signed-in
user is a member of the household that owns the resource**. Access control never
relies on ids being secret.

### 2. Medications belong to one household

`medications.household_id` is required. Each household keeps its own list; no
household ever sees another one's data. Storage locations follow the same rule.

### 3. Hard delete with cascade

Deleting is a real `DELETE`, with `ON DELETE CASCADE` down the ownership chain:
household → members, invites, locations, medications, preferences; medication →
batches → stock movements; user → memberships, invites they created, device
tokens, preferences.

Exceptions:

- `batches.storage_location_id` is `ON DELETE RESTRICT`: a location that still holds
  batches cannot be deleted, so no batch is lost by accident.
- `household_invites.used_by` is `ON DELETE SET NULL`.

A batch that runs out is **not** deleted: it stays with quantity 0 as history.
Deletion is for mistakes and for users who want their data gone (LGPD right to
erasure).

### 4. Stock movements: typed, signed, starting with `INITIAL`

`stock_movements` has `type` (`INITIAL`, `USE`, `DISCARD`, `ADJUSTMENT`) and a signed
`quantity_change` (`-2` for two tablets taken, `+5` for a count corrected upwards).
Creating a batch writes an `INITIAL` movement with its starting quantity, so for
every batch:

```
sum(quantity_change) = current_quantity
```

This invariant is checked in tests. `current_quantity` is still stored, so lists do
not need to sum the history. Movements store no user (non-negotiable rule 7).

### 5. Quantities: `NUMERIC(10,2)` / `BigDecimal`

`current_quantity`, `quantity_change` and `minimum_quantity` are `NUMERIC(10,2)`,
mapped to `BigDecimal`: syrups use 2.5 ml and tablets can be split in half.
`current_quantity` has `CHECK (current_quantity >= 0)`.

### 6. Enums: `VARCHAR` + `CHECK`, uppercase values

Enum columns (`role`, `form`, `unit`, `type`, `platform`, `frequency`) are
`VARCHAR` with a `CHECK` constraint listing the allowed values, mapped with
`@Enumerated(EnumType.STRING)`. Values are stored in uppercase, equal to the Java
constant names (`OWNER`, `DAILY`), so no converter is needed. Earlier ADRs write them
in lowercase; they mean the same values.

## Alternatives considered

- **UUID primary keys.** Not guessable, but larger and harder to read in logs and
  URLs. Guessing an id is harmless when every request checks membership.
- **Global medication catalog.** Avoids typing the same medicine twice, but raises
  "who can edit it" and mixes households' data. Autocomplete from ANVISA open data
  (roadmap idea) solves repetition without sharing.
- **Soft delete (`deleted_at`).** Keeps history, but every query must remember to
  filter deleted rows, a classic source of bugs, and "deleted" data is still stored.
- **No `INITIAL` movement.** One row less per batch, but the history no longer
  explains the current quantity on its own.
- **Native PostgreSQL enums.** Adding or renaming a value needs `ALTER TYPE`, which is
  awkward in migrations; `VARCHAR` + `CHECK` is changed with a simple new migration.
- **`INTEGER` quantities.** Cannot represent 2.5 ml or half a tablet.

## Consequences

- ✅ One consistent pattern for every table; the `V1` migration follows directly.
- ✅ Stock history can always be reconciled with the current quantity.
- ✅ Deleting data really deletes it, in line with LGPD.
- ⚠️ Membership checks are mandatory in every service method that loads household data;
  they need their own tests.
- ⚠️ Deleted data cannot be restored from the app; only database backups can.
- ⚠️ Deleting the account of a household's only owner would leave it with no owner.
  The service layer must prevent or handle this; to be decided with account deletion.
