package ru.phyllosedis.flashsale.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}