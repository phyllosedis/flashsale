package ru.phyllosedis.flashsale.user;

import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.shared.db.outbox.Outbox;
import ru.phyllosedis.flashsale.shared.db.outbox.OutboxJdbcRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OutboxJdbcRepository outboxJdbcRepository;
    private final ObjectMapper objectMapper;

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

        userRepository.save(user);

        outboxJdbcRepository.save(outbox);
    }

    public void register(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.email()) || userRepository.existsByPhone(dto.phone())) {
            return;
        }
        String salt = BCrypt.gensalt();
        String passwordHash = BCrypt.hashpw(dto.password(), salt);

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phone(dto.phone())
                .email(dto.email())
                .passwordHash(passwordHash)
                .build();

        save(user);
    }
}
