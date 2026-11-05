package ru.phyllosedis.flashsale.shared.db.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;

@Repository
@RequiredArgsConstructor
public class OutboxJdbcRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void save(Outbox outbox) {
        String sql = """
                INSERT INTO outbox(id, entity_id, event_type, payload)
                VALUES (:id, :entity_id, :event_type, :payload::jsonb);
                """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", outbox.getId())
                .addValue("entity_id", outbox.getEntityId())
                .addValue("event_type", outbox.getEventType())
                .addValue("payload", outbox.getPayload());
        jdbcTemplate.update(sql, sqlParameterSource);
    }

    /**
     * Deletes rows that Debezium has already captured. CDC consumers use the database
     * write-ahead log, so the outbox table is only a short-lived transport buffer and
     * should not grow forever.
     */
    public int deleteCreatedBefore(ZonedDateTime threshold) {
        String sql = "DELETE FROM outbox WHERE created_at < :threshold";
        return jdbcTemplate.update(sql, new MapSqlParameterSource("threshold", threshold));
    }
}
