package ru.phyllosedis.flashsale.listener.user;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.phyllosedis.flashsale.account.AccountService;
import ru.phyllosedis.flashsale.shared.lib.event.user.UserCreatedEvent;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
class UserCreatedListenerTest {

    private static final String TOPIC = "flashsale.user.outbox";

    @Autowired
    private UserCreatedListener listener;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AccountService accountService;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM inbox");
        reset(accountService);
    }

    @Test
    @DisplayName("Creates an account once when Kafka delivers the same event twice")
    void duplicateDeliveryIsIgnored() {
        String message = changeEvent(eventId, userId, "c", "user.created");

        listener.onUserCreated(record(message));
        listener.onUserCreated(record(message));

        verify(accountService, times(1)).createInitialAccount(userId);
        assertThat(inboxStatus(eventId)).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("Accepts a Debezium message wrapped in a schema/payload envelope")
    void schemaWrapperIsSupported() {
        Map<String, Object> envelope = envelope(eventId, userId, "c", "user.created");
        String message = write(Map.of("schema", Map.of(), "payload", envelope));

        listener.onUserCreated(record(message));

        verify(accountService).createInitialAccount(userId);
    }

    @Test
    @DisplayName("Ignores updates and deletes produced by the outbox table")
    void nonCreateOperationsAreIgnored() {
        listener.onUserCreated(record(changeEvent(eventId, userId, "u", "user.created")));

        verifyNoInteractions(accountService);
        assertThat(inboxRowCount()).isZero();
    }

    @Test
    @DisplayName("Ignores outbox rows of other event types")
    void otherEventTypesAreIgnored() {
        listener.onUserCreated(record(changeEvent(eventId, userId, "c", "user.updated")));

        verifyNoInteractions(accountService);
        assertThat(inboxRowCount()).isZero();
    }

    @Test
    @DisplayName("Supports the Debezium Outbox Event Router message shape")
    void outboxRouterShapeIsSupported() {
        UserCreatedEvent businessEvent =
                new UserCreatedEvent(userId, "user@example.com", "+79990000000", "Ivan", "Petrov");
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                TOPIC, 0, 0L, null, write(businessEvent));
        record.headers().add("id", eventId.toString().getBytes());
        record.headers().add("eventType", "user.created".getBytes());

        listener.onUserCreated(record);

        verify(accountService).createInitialAccount(userId);
        assertThat(inboxStatus(eventId)).isEqualTo("PROCESSED");
    }

    private String changeEvent(UUID eventId, UUID userId, String operation, String eventType) {
        return write(envelope(eventId, userId, operation, eventType));
    }

    private Map<String, Object> envelope(UUID eventId, UUID userId, String operation, String eventType) {
        UserCreatedEvent businessEvent =
                new UserCreatedEvent(userId, "user@example.com", "+79990000000", "Ivan", "Petrov");

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("id", eventId.toString());
        after.put("entity_id", userId.toString());
        after.put("event_type", eventType);
        after.put("status", "NEW");
        after.put("payload", write(businessEvent));

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("before", null);
        envelope.put("after", after);
        envelope.put("op", operation);
        return envelope;
    }

    private String write(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, null, value);
    }

    private String inboxStatus(UUID id) {
        return jdbcTemplate.queryForObject("SELECT status FROM inbox WHERE id = ?", String.class, id);
    }

    private int inboxRowCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM inbox", Integer.class);
        return count == null ? 0 : count;
    }
}
