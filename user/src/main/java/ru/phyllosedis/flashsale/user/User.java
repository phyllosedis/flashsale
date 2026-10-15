package ru.phyllosedis.flashsale.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"user\"")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "email", nullable = false, unique = true)
    String email;

    @Column(name = "phone", length = 12, nullable = false, unique = true)
    String phone;

    @Column(name = "last_name", nullable = false)
    String lastName;

    @Column(name = "first_name", nullable = false)
    String firstName;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(name = "created_at")
    ZonedDateTime createdAt;
}
