CREATE TABLE "event" (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL
);

CREATE TABLE booking (
    id SERIAL PRIMARY KEY
);

CREATE TABLE seat (
    id SERIAL PRIMARY KEY
);

CREATE TABLE users (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50)
);

CREATE TABLE account (
    id      SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    amount  NUMERIC(20, 2),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    created timestamp,
    amount NUMERIC(20,2)
)