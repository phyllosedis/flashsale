ALTER TABLE account
    ADD CONSTRAINT uq_account_user_id UNIQUE (user_id);
