package ru.phyllosedis.flashsale.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.transaction.Transaction;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionRepository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int MONEY_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final MoneyTransferService moneyTransferService;

    /**
     * Performs a transfer atomically: the transaction row and both account balances are
     * committed in the same database transaction. There is no PENDING state that could
     * leak to the outside world, because the whole operation is local to this database.
     */
    @Transactional
    public UUID makeTransaction(UUID from, UUID to, BigDecimal amount) {
        Objects.requireNonNull(from, "from account id must not be null");
        Objects.requireNonNull(to, "to account id must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (from.equals(to)) {
            return reject(from, to, amount, TransactionFailedReason.SAME_ACCOUNT);
        }
        if (amount.signum() <= 0 || amount.scale() > MONEY_SCALE) {
            return reject(from, to, amount, TransactionFailedReason.INVALID_MONEY_VALUE);
        }

        Transaction transaction = transactionRepository.save(Transaction.pending(from, to, amount));

        if (moneyTransferService.executeTransfer(from, to, amount)) {
            transaction.complete();
            log.info("Transaction {} completed: {} -> {}, amount={}", transaction.getId(), from, to, amount);
        } else {
            transaction.reject(TransactionFailedReason.NOT_ENOUGH_MONEY);
            log.info("Transaction {} rejected: not enough funds on account {}", transaction.getId(), from);
        }

        return transaction.getId();
    }

    private UUID reject(UUID from, UUID to, BigDecimal amount, TransactionFailedReason reason) {
        Transaction transaction = transactionRepository.save(Transaction.rejected(from, to, amount, reason));
        log.info("Transaction {} rejected before execution: reason={}", transaction.getId(), reason);
        return transaction.getId();
    }
}
