# ADR 0007: Definition of "low stock"

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #39

## Context

The MVP list shows three statuses: **expired**, **expires within 30 days** and
**low stock**. The first two come from dates. "Low stock" has no natural
definition: 10 tablets may be plenty of an occasional painkiller and too few of a
daily continuous-use medicine.

## Decision

### 1. Per-medication, opt-in minimum

New column on `medications`:

| Column | Type | Notes |
|---|---|---|
| `minimum_quantity` | numeric, nullable | same unit as the medication (`unit`) |

If `minimum_quantity` is `NULL`, the medication is **not** checked for low stock.
Users only set it for the medicines where running out matters.

### 2. Rule

A medication is **low stock** when:

```
sum(current_quantity of its active batches) < minimum_quantity
```

A batch is **active** when:

- its `current_quantity` is greater than zero, **and**
- it is not expired according to its **effective expiry** (the earlier of the
  printed expiry and `opened_at + shelf_life_after_opening_days`).

Expired boxes do not count as stock: a medicine that can no longer be used should
not hide the fact that the household needs to buy more.

This is pure logic, covered by unit tests, like the effective expiry rule.

## Alternatives considered

- **Global threshold (for example, fewer than 5 units).** One number cannot fit
  tablets, syrups (ml) and eye drops at the same time.
- **Minimum on every medication, required.** Forces the user to think about a
  number for every medicine at registration, which slows down the first use.
- **Forecast based on consumption rate.** More useful in theory, but needs usage
  history the app does not have yet, and it gets closer to treatment tracking,
  which the MVP avoids.

## Consequences

- ✅ Simple to explain and to test.
- ✅ No noise: medicines without a minimum never raise low-stock alerts.
- ✅ Expired boxes do not create a false sense of having enough.
- ⚠️ Users who never set a minimum never get low-stock alerts. The UI should make
  the field easy to find when editing a medication.
