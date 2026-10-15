package ru.phyllosedis.flashsale.user;

public record CreateUserDto(
        String firstName,
        String lastName,
        String phone,
        String email,
        String password) {
}
