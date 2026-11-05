CREATE TABLE inbox(
    id           uuid PRIMARY KEY,
    entity_id    uuid NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    status       VARCHAR(100) NOT NULL DEFAULT 'NEW',
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- A plain index is used instead of a partial index because the project also runs
-- Liquibase against H2 in PostgreSQL compatibility mode during tests.
CREATE INDEX idx_inbox_status ON inbox(status);
