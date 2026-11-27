package com.bazaarhub.backend.delivery.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class DeliveryAssignmentNotFoundException extends ResourceNotFoundException {

    public DeliveryAssignmentNotFoundException(String message) {
        super(message);
    }

    public DeliveryAssignmentNotFoundException(Long id) {
        super("Delivery assignment not found with ID: " + id);
    }
}
