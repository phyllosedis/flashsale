package ru.phyllosedis.flashsale.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import ru.phyllosedis.flashsale.shared.db.outbox.Outbox;
import ru.phyllosedis.flashsale.shared.db.outbox.OutboxJdbcRepository;
import ru.phyllosedis.flashsale.shared.lib.event.user.UserCreatedEvent;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEventService {

    private final ObjectMapper objectMapper;
    private final OutboxJdbcRepository outboxJdbcRepository;
    private final UserJdbcRepository userJdbcRepository;

    @Transactional
    public void save(User user) {
        UserCreatedEvent userCreatedEvent = new UserCreatedEvent(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFirstName(),
                user.getLastName());

        Outbox outbox = Outbox.builder()
                .id(UUID.randomUUID())
                .entityId(user.getId())
                .eventType("user.created")
                .payload(objectMapper.writeValueAsString(userCreatedEvent))
                .build();

        userJdbcRepository.save(user);

        outboxJdbcRepository.save(outbox);
    }
}
