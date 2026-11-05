package ru.phyllosedis.flashsale.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.account.Account;
import ru.phyllosedis.flashsale.account.AccountRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyTransferService {

    private final AccountRepository accountRepository;

    /**
     * Moves money inside the caller's transaction and reports the outcome instead of
     * throwing a business exception. Throwing an exception out of a transactional
     * boundary would mark the shared transaction as rollback-only and erase the
     * rejected transaction row.
     *
     * @return {@code true} when the transfer was applied, {@code false} when the source
     * account does not have enough money
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean executeTransfer(UUID from, UUID to, BigDecimal amount) {
        // Lock accounts in a stable order to avoid deadlocks between opposite transfers.
        UUID firstId = from.compareTo(to) < 0 ? from : to;
        UUID secondId = from.compareTo(to) < 0 ? to : from;

        Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Счёт не найден: " + firstId));
        Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Счёт не найден: " + secondId));

        Account accountFrom = from.equals(firstId) ? firstAccount : secondAccount;
        Account accountTo = to.equals(firstId) ? firstAccount : secondAccount;

        if (!accountFrom.hasAtLeast(amount)) {
            return false;
        }

        accountFrom.debit(amount);
        accountTo.credit(amount);

        log.debug("Transferred {} from {} to {}", amount, from, to);
        return true;
    }
}
