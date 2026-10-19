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
}
