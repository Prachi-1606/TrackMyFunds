-- TrackMyFunds — monthly budget tracking
-- One row per (category, month, year) — enforced by a unique constraint.
-- Columns are named *_value because `month` and `year` are reserved words
-- in several SQL dialects (notably H2 used in tests/dev).

CREATE TABLE budgets (
    id            BIGSERIAL       PRIMARY KEY,
    category      VARCHAR(20)     NOT NULL,
    monthly_limit DECIMAL(19, 2)  NOT NULL,
    month_value   INTEGER         NOT NULL,
    year_value    INTEGER         NOT NULL,
    created_at    TIMESTAMP,
    CONSTRAINT uk_budgets_category_month_year UNIQUE (category, month_value, year_value)
);

CREATE INDEX idx_budgets_month_year ON budgets (month_value, year_value);
