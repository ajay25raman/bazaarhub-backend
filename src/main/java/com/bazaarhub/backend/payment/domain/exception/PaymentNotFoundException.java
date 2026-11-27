package com.bazaarhub.backend.payment.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class PaymentNotFoundException extends ResourceNotFoundException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(Long paymentId) {
        super(String.format("Payment with ID %d was not found", paymentId));
    }

    public PaymentNotFoundException(String fieldName, String fieldValue) {
        super(String.format("Payment with %s '%s' was not found", fieldName, fieldValue));
    }
}
