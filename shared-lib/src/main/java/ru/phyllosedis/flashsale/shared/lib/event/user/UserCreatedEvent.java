package ru.phyllosedis.flashsale.shared.lib.event.user;

import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String email,
        String phone,
        String firstName,
        String lastName
) {
}
