package ru.phyllosedis.flashsale.user;

import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserEventService userEventService;

    public UUID register(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("User with email " + dto.email() + " already exists");
        }
        if (userRepository.existsByPhone(dto.phone())) {
            throw new UserAlreadyExistsException("User with phone " + dto.phone() + " already exists");
        }

        String passwordHash = BCrypt.hashpw(dto.password(), BCrypt.gensalt());

        User user = User.builder()
                .id(UUID.randomUUID())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phone(dto.phone())
                .email(dto.email())
                .passwordHash(passwordHash)
                .build();

        try {
            userEventService.save(user);
        } catch (DataIntegrityViolationException e) {
            // The unique constraints on email/phone are the source of truth; the checks
            // above only provide a friendly message in the common case. Two concurrent
            // registrations can still race here.
            throw new UserAlreadyExistsException("User with the same email or phone already exists");
        }

        return user.getId();
    }
}
