package ru.phyllosedis.flashsale.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.account.Account;
import ru.phyllosedis.flashsale.account.AccountRepository;
import ru.phyllosedis.flashsale.payment.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoneyTransferService {
    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeTransfer(UUID from, UUID to, BigDecimal amount) {
        // выясняем какой из них будет первым в очередь на блокировку
        UUID firstId = from.compareTo(to) < 0 ? from : to;
        UUID secondId = from.compareTo(to) < 0 ? to : from;

        Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Такого счёта нет"));
        Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Такого счёта нет"));

        // выясняем кто из них кто
        Account accountFrom = firstAccount.getId().equals(from) ? firstAccount : secondAccount;
        Account accountTo = firstAccount.getId().equals(to) ? firstAccount : secondAccount;

        if (accountFrom.getAmount().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на счёте");
        }

        accountTo.setAmount(accountTo.getAmount().add(amount));
        accountFrom.setAmount(accountFrom.getAmount().subtract(amount));

        accountRepository.save(accountFrom);
        accountRepository.save(accountTo);
    }
}
