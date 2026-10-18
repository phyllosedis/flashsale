CREATE TABLE outbox(
    id         uuid PRIMARY KEY,
    entity_id  uuid NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    status     VARCHAR(100) NOT NULL DEFAULT 'NEW',
    payload    jsonb NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_status ON outbox(status) WHERE (status = 'NEW');