package ru.phyllosedis.flashsale.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Boolean> createUser(@RequestBody CreateUserDto dto) {
        boolean register = userService.register(dto);
        return new ResponseEntity<>(register, register ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
