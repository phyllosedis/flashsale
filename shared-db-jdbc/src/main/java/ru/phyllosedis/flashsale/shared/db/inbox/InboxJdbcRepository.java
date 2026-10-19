package ru.phyllosedis.flashsale.shared.db.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InboxJdbcRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void save(Inbox inbox) {
        String sql = """
                INSERT INTO inbox(id, entity_id, event_type, status, created_at, processed_at)
                VALUES (:id, :entity_id, :event_type, :status, :created_at, :processed_at)
                """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("id", inbox.getId())
                .addValue("entity_id", inbox.getEntityId())
                .addValue("event_type", inbox.getEventType())
                .addValue("status", inbox.getStatus())
                .addValue("created_at", inbox.getCreatedAt())
                .addValue("processed_at", inbox.getProcessedAt());

        jdbcTemplate.update(sql, sqlParameterSource);
    }
}
