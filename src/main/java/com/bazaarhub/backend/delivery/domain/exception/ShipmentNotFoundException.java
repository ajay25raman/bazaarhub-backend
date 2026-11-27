package com.bazaarhub.backend.delivery.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class ShipmentNotFoundException extends ResourceNotFoundException {

    public ShipmentNotFoundException(String message) {
        super(message);
    }

    public ShipmentNotFoundException(Long id) {
        super("Shipment not found with ID: " + id);
    }
}
