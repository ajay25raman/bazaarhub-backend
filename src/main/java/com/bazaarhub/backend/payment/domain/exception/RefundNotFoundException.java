package com.bazaarhub.backend.payment.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class RefundNotFoundException extends ResourceNotFoundException {

    public RefundNotFoundException(String message) {
        super(message);
    }

    public RefundNotFoundException(Long refundId) {
        super(String.format("Refund with ID %d was not found", refundId));
    }

    public RefundNotFoundException(String fieldName, String fieldValue) {
        super(String.format("Refund with %s '%s' was not found", fieldName, fieldValue));
    }
}
