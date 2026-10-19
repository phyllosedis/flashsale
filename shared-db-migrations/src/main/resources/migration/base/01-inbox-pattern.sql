CREATE TABLE inbox(
    id           uuid PRIMARY KEY,
    entity_id    uuid NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    status       VARCHAR(100) NOT NULL DEFAULT 'NEW',
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_inbox_status ON inbox(status) WHERE (status = 'NEW');