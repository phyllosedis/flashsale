package ru.phyllosedis.flashsale.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.phyllosedis.flashsale.payment.exceptions.InsufficientFundsException;
import ru.phyllosedis.flashsale.transaction.Transaction;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionRepository;
import ru.phyllosedis.flashsale.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final MoneyTransferService moneyTransferService;

    public void makeTransaction(UUID from, UUID to, BigDecimal amount) {
        Transaction transaction = new Transaction();

        boolean validate = validate(transaction, from, to, amount);

        if (!validate) {
            return;
        }

        transaction.setStatus(TransactionStatus.NEW);
        transaction = transactionRepository.save(transaction);

        try {
            transaction.setStatus(TransactionStatus.PENDING);

            transactionRepository.save(transaction);

            moneyTransferService.executeTransfer(from, to, amount);

            transaction.setStatus(TransactionStatus.COMPLETED);
        } catch (InsufficientFundsException e) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setReason(TransactionFailedReason.NOT_ENOUGH_MONEY);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setReason(TransactionFailedReason.UNDEFINED);
        } finally {
            transactionRepository.save(transaction);
        }
    }

    private boolean validate(Transaction transaction, UUID from, UUID to, BigDecimal amount) {
        transaction.setFrom(from);
        transaction.setTo(to);
        transaction.setAmount(amount);

        if (amount.compareTo(new BigDecimal(0)) < 0) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setReason(TransactionFailedReason.INVALID_MONEY_VALUE);
            transactionRepository.save(transaction);
            return false;
        }
        if (from.equals(to)) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setReason(TransactionFailedReason.SAME_ACCOUNT);
            transactionRepository.save(transaction);
            return false;
        }
        return true;
    }
}
