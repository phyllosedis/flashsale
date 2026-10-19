package ru.phyllosedis.flashsale.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepository {
    private final NamedParameterJdbcTemplate template;

    public void save(User user) {
        String sql = """
                INSERT INTO "user"(id, email, phone, first_name, last_name, password_hash)
                VALUES (:id, :email, :phone, :first_name, :last_name, :password_hash);
                """;

        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("email", user.getEmail())
                .addValue("phone", user.getPhone())
                .addValue("first_name", user.getFirstName())
                .addValue("last_name", user.getLastName())
                .addValue("password_hash", user.getPasswordHash());

        template.update(sql, param);
    }
}
