package ru.phyllosedis.flashsale.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserEventService userEventService;

    public UUID register(CreateUserDto dto) throws JsonProcessingException {
        if (userRepository.existsByEmail(dto.email()) || userRepository.existsByPhone(dto.phone())) {
            return null;
        }
        String salt = BCrypt.gensalt();
        String passwordHash = BCrypt.hashpw(dto.password(), salt);

        User user = User.builder()
                .id(UUID.randomUUID())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phone(dto.phone())
                .email(dto.email())
                .passwordHash(passwordHash)
                .build();

        userEventService.save(user);
        return user.getId();
    }
}
