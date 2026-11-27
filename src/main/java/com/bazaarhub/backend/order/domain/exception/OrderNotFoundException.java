package com.bazaarhub.backend.order.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super("Order", orderId);
    }

    public OrderNotFoundException(String fieldName, Object fieldValue) {
        super(String.format("Order with %s '%s' was not found", fieldName, fieldValue));
    }
}
