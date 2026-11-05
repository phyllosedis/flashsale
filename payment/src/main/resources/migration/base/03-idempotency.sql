CREATE TABLE payment_idempotency(
    idempotency_key VARCHAR(128) PRIMARY KEY,
    transaction_id  uuid,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_idempotency_created_at ON payment_idempotency(created_at);
