package com.bazaarhub.backend.payment.domain.exception;

public class PaymentVerificationException extends RuntimeException {

    public PaymentVerificationException(String message) {
        super(message);
    }
}
