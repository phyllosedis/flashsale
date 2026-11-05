package ru.phyllosedis.flashsale.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public void createInitialAccount(UUID userId) {
        if (accountRepository.existsByUserId(userId)) {
            return;
        }
        accountRepository.save(new Account(userId));
    }
}
