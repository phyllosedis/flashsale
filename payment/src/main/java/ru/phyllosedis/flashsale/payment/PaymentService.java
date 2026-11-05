package ru.phyllosedis.flashsale.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.payment.exceptions.IdempotencyConflictException;
import ru.phyllosedis.flashsale.payment.idempotency.PaymentIdempotencyService;
import ru.phyllosedis.flashsale.transaction.Transaction;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionRepository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int MONEY_SCALE = 2;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final TransactionRepository transactionRepository;
    private final MoneyTransferService moneyTransferService;
    private final PaymentIdempotencyService idempotencyService;

    @Transactional
    public PaymentResult makeTransaction(UUID from, UUID to, BigDecimal amount) {
        return makeTransaction(from, to, amount, null);
    }

    /**
     * Performs a transfer atomically: the transaction row and both account balances are
     * committed in the same database transaction. There is no PENDING state that could
     * leak to the outside world, because the whole operation is local to this database.
     *
     * <p>When an idempotency key is supplied, a replay returns the result of the original
     * transaction instead of moving money twice.
     */
    @Transactional
    public PaymentResult makeTransaction(UUID from, UUID to, BigDecimal amount, String idempotencyKey) {
        Objects.requireNonNull(from, "from account id must not be null");
        Objects.requireNonNull(to, "to account id must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        String key = normalizeKey(idempotencyKey);
        if (key != null) {
            Optional<PaymentResult> replay = idempotencyService.findResult(key);
            if (replay.isPresent()) {
                log.info("Replaying transaction {} for idempotency key {}", replay.get().transactionId(), key);
                return replay.get();
            }
            if (!idempotencyService.reserve(key)) {
                // Another request reserved the key but has not committed its result yet.
                throw new IdempotencyConflictException(key);
            }
        }

        PaymentResult result = execute(from, to, amount);

        if (key != null) {
            idempotencyService.complete(key, result.transactionId());
        }
        return result;
    }

    private PaymentResult execute(UUID from, UUID to, BigDecimal amount) {
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

        return PaymentResult.from(transaction);
    }

    private PaymentResult reject(UUID from, UUID to, BigDecimal amount, TransactionFailedReason reason) {
        Transaction transaction = transactionRepository.save(Transaction.rejected(from, to, amount, reason));
        log.info("Transaction {} rejected before execution: reason={}", transaction.getId(), reason);
        return PaymentResult.from(transaction);
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must not be longer than " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
        return trimmed;
    }
}
