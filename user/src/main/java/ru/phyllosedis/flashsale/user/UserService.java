package ru.phyllosedis.flashsale.user;

import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public boolean register(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.email()) || userRepository.existsByPhone(dto.phone())) {
            return false;
        }
        String salt = BCrypt.gensalt();
        String passwordHash = BCrypt.hashpw(dto.password(), salt);

        User user = User.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phone(dto.phone())
                .email(dto.email())
                .passwordHash(passwordHash)
                .build();

        userRepository.save(user);

        return user.getId() != null;
    }
}
