package ru.phyllosedis.flashsale.payment.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.phyllosedis.flashsale.payment.PaymentResult;
import ru.phyllosedis.flashsale.transaction.TransactionRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentIdempotencyService {

    private final PaymentIdempotencyJdbcRepository idempotencyRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PaymentResult> findResult(String key) {
        return idempotencyRepository.findTransactionId(key)
                .flatMap(transactionRepository::findById)
                .map(PaymentResult::from);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean reserve(String key) {
        return idempotencyRepository.tryReserve(key);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(String key, UUID transactionId) {
        idempotencyRepository.complete(key, transactionId);
    }
}
