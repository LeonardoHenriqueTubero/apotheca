# ER diagram

The database schema of Apotheca, implemented by the Flyway migrations in
`backend/src/main/resources/db/migration/`. Conventions (ids, enums, quantities,
deletion) are in [ADR 0011](../decisions/0011-data-model-conventions.md); date and time
types in [ADR 0010](../decisions/0010-date-and-time-handling.md).

```mermaid
erDiagram
    users ||--o{ household_members : "belongs to"
    households ||--o{ household_members : has
    households ||--o{ household_invites : has
    users ||--o{ household_invites : "creates / uses"
    households ||--o{ storage_locations : has
    households ||--o{ medications : owns
    medications ||--o{ batches : "bought as"
    storage_locations ||--o{ batches : stores
    batches ||--o{ stock_movements : records
    users ||--o{ fcm_device_tokens : registers
    users ||--o{ notification_preferences : sets
    households ||--o{ notification_preferences : "alerts for"

    users {
        bigint id PK
        varchar firebase_uid UK
        varchar email
        varchar display_name "nullable"
        timestamptz created_at
    }

    households {
        bigint id PK
        varchar name
        timestamptz created_at
    }

    household_members {
        bigint id PK
        bigint household_id FK
        bigint user_id FK
        varchar role "OWNER | MEMBER"
        timestamptz joined_at
    }

    household_invites {
        bigint id PK
        bigint household_id FK
        varchar token UK
        bigint created_by FK
        timestamptz created_at
        timestamptz expires_at "created_at + 24h"
        timestamptz used_at "nullable"
        bigint used_by FK "nullable"
    }

    storage_locations {
        bigint id PK
        bigint household_id FK
        varchar name
        timestamptz created_at
    }

    medications {
        bigint id PK
        bigint household_id FK
        varchar name
        varchar active_ingredient "nullable"
        varchar strength "nullable, e.g. 500 mg"
        varchar form "TABLET | CAPSULE | SYRUP | DROPS | ..."
        varchar unit "UNIT | ML | G"
        int shelf_life_after_opening_days "nullable"
        numeric minimum_quantity "nullable, ADR 0007"
        timestamptz created_at
    }

    batches {
        bigint id PK
        bigint medication_id FK
        bigint storage_location_id FK
        date expiration_date
        numeric current_quantity ">= 0"
        date opened_at "nullable"
        timestamptz created_at
    }

    stock_movements {
        bigint id PK
        bigint batch_id FK
        varchar type "INITIAL | USE | DISCARD | ADJUSTMENT"
        numeric quantity_change "signed"
        timestamptz occurred_at
    }

    fcm_device_tokens {
        bigint id PK
        bigint user_id FK
        varchar token UK
        varchar platform "WEB | ANDROID | IOS"
        timestamptz created_at
        timestamptz last_used_at
    }

    notification_preferences {
        bigint id PK
        bigint user_id FK
        bigint household_id FK
        varchar frequency "OFF | DAILY | WEEKLY"
        timestamptz last_sent_at "nullable"
    }
```

## Notes

- **Unique pairs:** `household_members (household_id, user_id)`,
  `notification_preferences (user_id, household_id)`,
  `storage_locations (household_id, name)`.
- **Deletion:** deleting a household removes everything inside it; deleting a
  medication removes its batches and movements (`ON DELETE CASCADE`). A storage
  location that still holds batches cannot be deleted (`ON DELETE RESTRICT`); move
  the batches first. `household_invites.used_by` becomes `NULL` if that user is deleted.
- **Invariant:** for every batch, `sum(stock_movements.quantity_change) = current_quantity`.
  Creating a batch writes an `INITIAL` movement.
- **Privacy (LGPD):** `stock_movements` has no user column, on purpose: the app does
  not record *who* took a medicine.
