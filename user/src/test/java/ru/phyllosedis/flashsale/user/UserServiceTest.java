package ru.phyllosedis.flashsale.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserEventService userEventService;

    @InjectMocks
    private UserService userService;

    private final CreateUserDto dto = new CreateUserDto(
            "Ivan", "Petrov", "+79990000000", "ivan@example.com", "password123");

    @Test
    @DisplayName("Registers a user and writes a user.created outbox event")
    void registersUser() {
        UUID userId = userService.register(dto);

        assertThat(userId).isNotNull();
        verify(userEventService).save(any(User.class));
    }

    @Test
    @DisplayName("Rejects a duplicate email before writing anything")
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userEventService, never()).save(any());
    }

    @Test
    @DisplayName("Rejects a duplicate phone before writing anything")
    void rejectsDuplicatePhone() {
        when(userRepository.existsByPhone(dto.phone())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("phone");

        verify(userEventService, never()).save(any());
    }

    @Test
    @DisplayName("Translates a unique constraint race into a domain error")
    void translatesUniqueConstraintViolation() {
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(userEventService).save(any(User.class));

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(UserAlreadyExistsException.class);
    }
}
