-- TrackMyFunds — initial schema (PostgreSQL)
-- Column names follow Spring's SpringPhysicalNamingStrategy (camelCase → snake_case).
-- Enum columns are VARCHAR so new enum values never require a DDL change.

CREATE TABLE expenses (
    id             BIGSERIAL       PRIMARY KEY,
    title          VARCHAR(255)    NOT NULL,
    amount         DECIMAL(19, 2)  NOT NULL,
    category       VARCHAR(20)     NOT NULL,
    payment_method VARCHAR(20)     NOT NULL,
    date           DATE            NOT NULL,
    description    TEXT,
    created_at     TIMESTAMP
);

-- Indexes for the most common filter/sort columns
CREATE INDEX idx_expenses_date     ON expenses (date);
CREATE INDEX idx_expenses_category ON expenses (category);
