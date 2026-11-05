package ru.phyllosedis.flashsale.payment.api;

import ru.phyllosedis.flashsale.payment.PaymentResult;

import java.util.UUID;

public record PaymentResponse(
        UUID transactionId,
        String status,
        String reason) {

    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
                result.transactionId(),
                result.status().name(),
                result.reason() == null ? null : result.reason().name());
    }
}
