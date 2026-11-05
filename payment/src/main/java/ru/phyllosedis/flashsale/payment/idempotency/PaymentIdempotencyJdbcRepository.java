package ru.phyllosedis.flashsale.payment.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentIdempotencyJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Reserves the key for the current transaction. Returns {@code false} when the key
     * already exists. The caller must be inside the same transaction as the payment so
     * that a failed payment removes the reservation.
     */
    public boolean tryReserve(String key) {
        String sql = """
                INSERT INTO payment_idempotency(idempotency_key)
                SELECT :key
                WHERE NOT EXISTS (SELECT 1 FROM payment_idempotency WHERE idempotency_key = :key)
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource("key", key)) > 0;
    }

    public Optional<UUID> findTransactionId(String key) {
        String sql = """
                SELECT transaction_id
                FROM payment_idempotency
                WHERE idempotency_key = :key
                """;
        List<UUID> ids = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("key", key),
                (rs, rowNum) -> {
                    String value = rs.getString("transaction_id");
                    return value == null ? null : UUID.fromString(value);
                });
        return ids.stream().filter(Objects::nonNull).findFirst();
    }

    public void complete(String key, UUID transactionId) {
        String sql = """
                UPDATE payment_idempotency
                SET transaction_id = :transaction_id
                WHERE idempotency_key = :key
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("key", key)
                .addValue("transaction_id", transactionId));
    }
}
