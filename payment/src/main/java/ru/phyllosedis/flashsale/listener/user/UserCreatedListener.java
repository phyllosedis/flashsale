package ru.phyllosedis.flashsale.listener.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.account.AccountService;
import ru.phyllosedis.flashsale.shared.db.inbox.InboxJdbcRepository;
import ru.phyllosedis.flashsale.shared.lib.event.user.UserCreatedEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreatedListener {

    private static final String USER_CREATED_EVENT = "user.created";
    private static final String CREATE_OPERATION = "c";

    private final ObjectMapper objectMapper;
    private final AccountService accountService;
    private final InboxJdbcRepository inboxJdbcRepository;

    /**
     * Consumes both raw Debezium change events (default connector setup in
     * {@code infra/debezium}) and messages produced by the Debezium Outbox Event Router.
     *
     * <p>The whole method is one database transaction: the inbox marker, the account
     * insert and the "processed" flag are committed together. A failure rolls the marker
     * back, so Kafka can safely redeliver the message.
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topics.user-created:flashsale.user.outbox}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}")
    public void onUserCreated(ConsumerRecord<String, String> record) {
        String message = record.value();
        if (message == null || message.isBlank()) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode node = unwrapSchema(root);
            IncomingEvent event = node.has("after") ? fromChangeEvent(node) : fromOutboxRouter(record, node);

            if (event == null || !USER_CREATED_EVENT.equals(event.eventType())) {
                return;
            }

            if (!inboxJdbcRepository.tryInsert(event.id(), event.entityId(), event.eventType())) {
                log.debug("Event {} was already processed, skipping duplicate delivery", event.id());
                return;
            }

            UserCreatedEvent businessEvent = objectMapper.readValue(event.payload(), UserCreatedEvent.class);
            accountService.createInitialAccount(businessEvent.userId());
            inboxJdbcRepository.markProcessed(event.id());

            log.info("Initial account created for user {} (event {})", businessEvent.userId(), event.id());
        } catch (JacksonException e) {
            // Rethrowing hands the message over to Kafka's retry/DLT machinery. Swallowing
            // parsing errors here was the reason malformed events disappeared silently.
            throw new IllegalArgumentException("Cannot parse user-created Kafka message", e);
        }
    }

    /**
     * With {@code value.converter.schemas.enable=true} Debezium wraps the change event in
     * {@code {"schema": ..., "payload": ...}}. The connector in this repository disables
     * the wrapper, but handling both shapes keeps the listener tolerant to config drift.
     */
    private JsonNode unwrapSchema(JsonNode root) {
        JsonNode payload = root.get("payload");
        return payload != null && payload.isObject() ? payload : root;
    }

    private IncomingEvent fromChangeEvent(JsonNode node) {
        if (!CREATE_OPERATION.equals(node.path("op").asText())) {
            return null;
        }

        JsonNode after = node.get("after");
        if (after == null || after.isNull()) {
            return null;
        }

        JsonNode payload = after.path("payload");
        String payloadJson = payload.isTextual() ? payload.asText() : payload.toString();

        return new IncomingEvent(
                requiredUuid(after, "id"),
                requiredUuid(after, "entity_id"),
                after.path("event_type").asText(),
                payloadJson);
    }

    private IncomingEvent fromOutboxRouter(ConsumerRecord<String, String> record, JsonNode node) {
        String eventId = header(record, "id");
        String eventType = header(record, "eventType");
        if (eventId == null || eventType == null) {
            throw new IllegalArgumentException(
                    "Unsupported Kafka message: neither a Debezium change event nor an outbox event with headers");
        }

        UUID id = UUID.fromString(eventId);
        return new IncomingEvent(id, id, eventType, node.toString());
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static UUID requiredUuid(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing required outbox field: " + field);
        }
        return UUID.fromString(value.asText());
    }

    private record IncomingEvent(UUID id, UUID entityId, String eventType, String payload) {
    }
}
