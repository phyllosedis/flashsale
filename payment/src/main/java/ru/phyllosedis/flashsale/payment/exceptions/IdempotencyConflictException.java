package ru.phyllosedis.flashsale.payment.exceptions;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("A request with idempotency key '" + key + "' is already in progress");
    }
}
