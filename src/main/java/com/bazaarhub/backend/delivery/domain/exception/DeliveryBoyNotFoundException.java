package com.bazaarhub.backend.delivery.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class DeliveryBoyNotFoundException extends ResourceNotFoundException {

    public DeliveryBoyNotFoundException(String message) {
        super(message);
    }

    public DeliveryBoyNotFoundException(Long id) {
        super("Delivery boy not found with ID: " + id);
    }
}
