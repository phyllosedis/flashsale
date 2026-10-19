package ru.phyllosedis.flashsale.listener.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.phyllosedis.flashsale.shared.lib.event.cdc.DebeziumEventEnvelope;
import ru.phyllosedis.flashsale.shared.lib.event.cdc.OutboxCdcPayload;
import ru.phyllosedis.flashsale.shared.lib.event.user.UserCreatedEvent;

import java.lang.reflect.Type;

@Service
@RequiredArgsConstructor
public class UserCreatedListener {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener()
    public void onUserCreated(String message) throws JsonProcessingException {
        DebeziumEventEnvelope<OutboxCdcPayload> cdcEnvelope = objectMapper.readValue(
                message,
                new TypeReference<DebeziumEventEnvelope<OutboxCdcPayload>>() {}
        );
        if (cdcEnvelope.after() == null || !"c".equals(cdcEnvelope.op())) {
            return;
        }

        OutboxCdcPayload outboxRow = cdcEnvelope.after();
        if ("user.created".equals(outboxRow.event_type())) {
            UserCreatedEvent businessEvent = objectMapper.readValue(
                    outboxRow.payload(),
                    UserCreatedEvent.class
            );
        }

    }
}
