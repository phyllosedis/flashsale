# AGENTS.md

This file is the hand-off document for the next AI agent (or human) working on
`flashsale`. Read it before changing anything. It describes the current state, the
decisions that were already made, and the work that is still open.

## 1. What this project is

`flashsale` is a Spring Boot 4 / Java 21 multi-module Maven project for a flash-sale
marketplace. The long-term flow is:

```
gateway -> user service  -> outbox -> Debezium -> Kafka -> payment service
        -> order service -> outbox -> Debezium -> Kafka -> payment/other consumers
        -> event service
```

Only `user` and `payment` have real business logic today. `gateway` has routing only.
`order` and `event` are still stubs.

## 2. Module map

| Module | Status | Notes |
| --- | --- | --- |
| `shared-lib` | partial | Event DTOs (`UserCreatedEvent`, `DebeziumEventEnvelope`, `OutboxCdcPayload`). |
| `shared-db-migrations` | active | Liquibase SQL for `outbox` and `inbox`, reused by services. |
| `shared-db-jdbc` | active | JDBC repositories for outbox/inbox. |
| `user` | working | Registration + outbox write + outbox retention job. |
| `payment` | working | Account creation consumer, inbox dedupe, atomic transfer. No REST API yet. |
| `gateway` | skeleton | Spring Cloud Gateway routes via `GatewayRoutes`. |
| `order` / `event` | stub | Do not build product features here without a design. |

## 3. Build and test

The build must work on JDK 21 through JDK 25. JDK 23+ no longer runs annotation
processors from the classpath implicitly, so Lombok is declared in
`maven-compiler-plugin/annotationProcessorPaths` in the root `pom.xml`. Do not remove
that block or the build will fail on modern JDKs with "cannot find symbol getId()".

```bash
./mvnw -DskipTests compile
./mvnw test
./mvnw -pl payment -am test      # one module plus its dependencies
```

`mvn` 3.9+ is required (enforced by `maven-enforcer-plugin`). The Maven wrapper
(`mvnw`, `mvnw.cmd`, `.mvn/wrapper`) is committed and pins Maven 3.9.11.

Tests currently use H2 in PostgreSQL compatibility mode. Docker/Testcontainers are
not available on the original development machine, so integration tests must stay
runnable without Docker.

## 4. Runtime configuration

Nothing may contain hard-coded hosts or credentials. Services read:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONSUMER_GROUP`,
  `KAFKA_TOPIC_USER_CREATED`
- `SERVER_PORT`, `USER_SERVICE_URI`, `PAYMENT_SERVICE_URI`

Defaults exist for local development (`localhost:5432`, `localhost:9092`).

## 5. Local infrastructure

`docker-compose.yaml` starts:

- PostgreSQL 17 with `wal_level=logical` and two databases created by
  `infra/postgres/init.sql` (`flashsale_user`, `flashsale_payment`).
- Kafka 8.x in KRaft mode.
- Debezium Connect (`quay.io/debezium/connect:3.7.0.Final`).
- `connect-init`, which registers every `infra/debezium/*-connector.json`.
- Kafka UI at <http://localhost:8989>.

The user outbox connector routes `flashsale-user.public.outbox` to the topic
`flashsale.user.outbox` and disables the Debezium `schema`/`payload` wrapper
(`value.converter.schemas.enable=false`). The payment listener supports both the
wrapped and unwrapped shape anyway.

```bash
docker compose up -d
docker compose logs -f connect-init
```

Docker is not installed on the original machine, so this compose stack has not been
executed there. Treat it as code-reviewed but unverified until someone runs it.

## 6. Domain invariants already implemented

### Payment

- `PaymentService.makeTransaction` is the transaction boundary. The transaction row,
  both account changes and the inbox marker commit together.
- `MoneyTransferService` uses `Propagation.MANDATORY` and **returns a boolean** instead
  of throwing `InsufficientFundsException`. Do not change it back to "throw and catch":
  a runtime exception escaping a transactional boundary marks the shared transaction
  rollback-only and deletes the rejected transaction row.
- Accounts are locked with `PESSIMISTIC_WRITE` in a stable UUID order to avoid
  deadlocks between opposite transfers.
- `Account` has no setters. Use `withBalance` for tests/bootstrap and
  `debit`/`credit` in business code.
- Money is rejected when `amount <= 0` or `amount.scale() > 2`.

### User

- `UserService.register` throws `UserAlreadyExistsException` (HTTP 409) instead of
  returning `null`.
- `UserEventService.save` writes the user and the outbox row in one transaction.
- `OutboxCleanupJob` deletes outbox rows older than `OUTBOX_RETENTION_DAYS` (default 7).

### Inbox / idempotency

- `InboxJdbcRepository.tryInsert` uses
  `INSERT ... SELECT ... WHERE NOT EXISTS`; H2 does not support PostgreSQL partial
  indexes or `ON CONFLICT`, which is why the shared SQL uses plain indexes.
- The listener inserts the inbox marker in the same transaction as the business work.
  If the work fails, the marker rolls back and Kafka can redeliver.

## 7. Known pitfalls

1. **Jackson 3, not Jackson 2.** Spring Boot 4 auto-configures
   `tools.jackson.databind.ObjectMapper`. Do not import
   `com.fasterxml.jackson.databind.*`; inject the `tools.jackson` mapper.
   `JacksonException` is a `RuntimeException`, so `throws JsonProcessingException`
   is no longer needed.
2. **Debezium payload shape.** A JSONB `outbox.payload` column arrives as a JSON
   string. The listener handles both a string and an object, but do not assume the
   outbox row is the business event itself unless the Event Router transform is used.
3. **H2 dialect.** H2 in PostgreSQL mode supports `jsonb` as a type and
   `WHERE NOT EXISTS`, but not partial indexes (`... WHERE status = 'NEW'`) or
   `ON CONFLICT`. Keep shared migrations portable.
4. **Don't catch exceptions across `@Transactional` boundaries** unless the
   transaction is intentionally discarded. See the payment transfer note above.
5. **`payment/pom.xml` and `user/pom.xml` already get Jackson from
   `spring-boot-starter-web`.** Do not re-add `jackson-databind`.
6. **Lombok.** Keep the annotation processor path in the root POM. IDE builds on
   JDK 21 may hide the problem until CI runs on JDK 25.

## 8. Current test coverage

- `PaymentServiceTest` — validation rules, rejection mapping.
- `PaymentServiceConcurrencyTest` — 100 concurrent transfers, overdraft protection.
- `UserCreatedListenerTest` — CDC envelope, schema wrapper, Event Router shape,
  duplicate delivery, ignored operations/event types.
- `UserServiceTest` — registration, duplicate email/phone, unique-constraint race.

## 9. Open work (recommended order)

1. **Payment REST API + idempotency** (next):
   - `POST /api/v1/payments` with an optional `Idempotency-Key` header.
   - Store the key in a `payment_idempotency` table and return the original result on
     replay.
   - Map rejected transfers to a clear Problem Details response (RFC 7807).
2. **Order vertical slice**: order creation, stock reservation, `order.created` outbox
   event, payment consumer, order status transitions.
3. **Gateway security**: JWT validation filter, route-level auth, rate limiting.
4. **Observability**: Micrometer timers for transfers, structured logs with
   transaction ids, health indicators for Kafka/DB.
5. **CI**: GitHub Actions running `./mvnw verify` on JDK 21 and 25, plus Testcontainers
   integration tests once Docker is available.
6. **Architecture tests**: ArchUnit rules for "domain does not depend on web",
   "no `com.fasterxml.jackson`", package cycles.
7. **Docs**: human-facing `README.md`, ADRs for outbox/inbox, API examples.

## 10. Conventions

- Commit messages: Conventional Commits (`fix(payment): ...`, `feat(user): ...`).
- Do not commit `.env`, `*.log`, IDE files, or credentials.
- Prefer constructor injection (`@RequiredArgsConstructor`) and records for DTOs.
- Keep business invariants inside aggregates (`Account`, `Transaction`), not in
  controllers.
- When adding a migration, never edit an already applied changeset; add a new one.
- Run `./mvnw test` before every commit.
