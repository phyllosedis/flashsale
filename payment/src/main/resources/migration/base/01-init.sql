CREATE TABLE account(
    id      uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    amount  NUMERIC(20, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_account_user_id ON account(user_id);

CREATE TABLE "transaction"(
    id              uuid PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_from    uuid NOT NULL,
    account_to      uuid NOT NULL,
    amount          NUMERIC(20, 2) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    reason          VARCHAR(255)
);

CREATE INDEX idx_transaction_account_from ON "transaction"(account_from, created_at DESC);

CREATE INDEX idx_transaction_account_to ON "transaction"(account_to, created_at DESC);