CREATE TABLE users (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    firebase_uid VARCHAR(128) NOT NULL UNIQUE,
    email        VARCHAR(320) NOT NULL,
    display_name VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE households (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE household_members (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (household_id, user_id)
);

CREATE INDEX idx_household_members_user_id ON household_members (user_id);

CREATE TABLE household_invites (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    token        VARCHAR(64) NOT NULL UNIQUE,
    created_by   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    used_by      BIGINT      REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_household_invites_household_id ON household_invites (household_id);
CREATE INDEX idx_household_invites_created_by ON household_invites (created_by);
CREATE INDEX idx_household_invites_used_by ON household_invites (used_by);

CREATE TABLE storage_locations (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id BIGINT       NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (household_id, name)
);

CREATE TABLE medications (
    id                            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    household_id                  BIGINT        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    name                          VARCHAR(150)  NOT NULL,
    active_ingredient             VARCHAR(150),
    strength                      VARCHAR(50),
    form                          VARCHAR(20)   NOT NULL CHECK (form IN (
                                      'TABLET', 'CAPSULE', 'SYRUP', 'SUSPENSION', 'DROPS',
                                      'CREAM', 'OINTMENT', 'GEL', 'SPRAY', 'INHALER',
                                      'INJECTION', 'POWDER', 'SUPPOSITORY', 'OTHER')),
    unit                          VARCHAR(10)   NOT NULL CHECK (unit IN ('UNIT', 'ML', 'G')),
    shelf_life_after_opening_days INTEGER       CHECK (shelf_life_after_opening_days > 0),
    minimum_quantity              NUMERIC(10,2) CHECK (minimum_quantity > 0),
    created_at                    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_medications_household_id ON medications (household_id);

CREATE TABLE batches (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    medication_id       BIGINT        NOT NULL REFERENCES medications (id) ON DELETE CASCADE,
    storage_location_id BIGINT        NOT NULL REFERENCES storage_locations (id) ON DELETE RESTRICT,
    expiration_date     DATE          NOT NULL,
    current_quantity    NUMERIC(10,2) NOT NULL CHECK (current_quantity >= 0),
    opened_at           DATE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_batches_medication_id ON batches (medication_id);
CREATE INDEX idx_batches_storage_location_id ON batches (storage_location_id);

CREATE TABLE stock_movements (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_id        BIGINT        NOT NULL REFERENCES batches (id) ON DELETE CASCADE,
    type            VARCHAR(20)   NOT NULL CHECK (type IN ('INITIAL', 'USE', 'DISCARD', 'ADJUSTMENT')),
    quantity_change NUMERIC(10,2) NOT NULL CHECK (quantity_change <> 0),
    occurred_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_movements_batch_id ON stock_movements (batch_id);

CREATE TABLE fcm_device_tokens (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token        VARCHAR(512) NOT NULL UNIQUE,
    platform     VARCHAR(10)  NOT NULL CHECK (platform IN ('WEB', 'ANDROID', 'IOS')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_fcm_device_tokens_user_id ON fcm_device_tokens (user_id);

CREATE TABLE notification_preferences (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    household_id BIGINT      NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    frequency    VARCHAR(10) NOT NULL DEFAULT 'DAILY' CHECK (frequency IN ('OFF', 'DAILY', 'WEEKLY')),
    last_sent_at TIMESTAMPTZ,
    UNIQUE (user_id, household_id)
);

CREATE INDEX idx_notification_preferences_household_id ON notification_preferences (household_id);
