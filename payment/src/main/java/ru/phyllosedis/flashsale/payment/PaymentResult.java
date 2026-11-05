package ru.phyllosedis.flashsale.payment;

import ru.phyllosedis.flashsale.transaction.Transaction;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionStatus;

import java.util.UUID;

public record PaymentResult(
        UUID transactionId,
        TransactionStatus status,
        TransactionFailedReason reason) {

    public static PaymentResult from(Transaction transaction) {
        return new PaymentResult(transaction.getId(), transaction.getStatus(), transaction.getReason());
    }

    public boolean completed() {
        return status == TransactionStatus.COMPLETED;
    }
}
