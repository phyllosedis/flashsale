package ru.phyllosedis.flashsale.shared.db.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InboxJdbcRepository {

    private static final String PROCESSING = "PROCESSING";
    private static final String PROCESSED = "PROCESSED";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Inserts a new inbox marker and returns {@code true} when this delivery won the
     * race. The statement is intentionally idempotent: the unique primary key plus
     * {@code WHERE NOT EXISTS} keeps repeated Kafka deliveries from doing the work twice.
     *
     * <p>The caller must invoke this method inside the same transaction as the business
     * work. If the work fails, the marker is rolled back and the message can be retried.
     */
    public boolean tryInsert(UUID id, UUID entityId, String eventType) {
        String sql = """
                INSERT INTO inbox(id, entity_id, event_type, status)
                SELECT :id, :entity_id, :event_type, :status
                WHERE NOT EXISTS (SELECT 1 FROM inbox WHERE id = :id)
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("entity_id", entityId)
                .addValue("event_type", eventType)
                .addValue("status", PROCESSING);
        return jdbcTemplate.update(sql, parameters) > 0;
    }

    public void markProcessed(UUID id) {
        String sql = """
                UPDATE inbox
                SET status = :status, processed_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", PROCESSED);
        jdbcTemplate.update(sql, parameters);
    }

    public int deleteProcessedBefore(ZonedDateTime threshold) {
        String sql = """
                DELETE FROM inbox
                WHERE status = :status AND processed_at < :threshold
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("status", PROCESSED)
                .addValue("threshold", threshold);
        return jdbcTemplate.update(sql, parameters);
    }
}
