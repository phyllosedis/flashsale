package ru.phyllosedis.flashsale.shared.db.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxJdbcRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void save(Outbox outbox) {
        String sql = """
                INSERT INTO outbox(id, event_type, status, payload, created_at)
                VALUES (:id, :event_type, :status, :payload::jsonb, :created_at);
                """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", outbox.getId())
                .addValue("event_type", outbox.getEventType())
                .addValue("status", outbox.getStatus())
                .addValue("payload", outbox.getPayload())
                .addValue("created_at", outbox.getCreatedAt());
        jdbcTemplate.update(sql, sqlParameterSource);
    }
}
