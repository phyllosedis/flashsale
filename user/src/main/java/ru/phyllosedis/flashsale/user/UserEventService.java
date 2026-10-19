package ru.phyllosedis.flashsale.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.shared.db.outbox.Outbox;
import ru.phyllosedis.flashsale.shared.db.outbox.OutboxJdbcRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEventService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OutboxJdbcRepository outboxJdbcRepository;
    private final UserJdbcRepository userJdbcRepository;
    private final UserRepository userRepository;

    @Transactional
    public void save(User user) throws JsonProcessingException {
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
