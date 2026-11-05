package ru.phyllosedis.flashsale.listener.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.phyllosedis.flashsale.account.AccountService;
import ru.phyllosedis.flashsale.shared.lib.event.cdc.DebeziumEventEnvelope;
import ru.phyllosedis.flashsale.shared.lib.event.cdc.OutboxCdcPayload;
import ru.phyllosedis.flashsale.shared.lib.event.user.UserCreatedEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreatedListener {

    private static final String USER_CREATED_EVENT = "user.created";
    private static final String CREATE_OPERATION = "c";

    private final ObjectMapper objectMapper;
    private final AccountService accountService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-created}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onUserCreated(String message) {
        try {
            DebeziumEventEnvelope<OutboxCdcPayload> cdcEnvelope = objectMapper.readValue(
                    message,
                    new TypeReference<DebeziumEventEnvelope<OutboxCdcPayload>>() {
                    });

            if (cdcEnvelope.after() == null || !CREATE_OPERATION.equals(cdcEnvelope.op())) {
                return;
            }

            OutboxCdcPayload outboxRow = cdcEnvelope.after();
            if (!USER_CREATED_EVENT.equals(outboxRow.event_type())) {
                return;
            }

            UserCreatedEvent businessEvent = objectMapper.readValue(
                    outboxRow.payload(),
                    UserCreatedEvent.class);

            accountService.createInitialAccount(businessEvent.userId());
            log.info("Initial account created for user {}", businessEvent.userId());
        } catch (JacksonException e) {
            // Throwing lets Kafka's error handler / retry policy decide what to do next.
            throw new IllegalArgumentException("Cannot parse user-created CDC message", e);
        }
    }
}
