package com.bazaarhub.backend.shipping.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class ShipmentAlreadyExistsException extends DomainException {
    public ShipmentAlreadyExistsException(String message) {
        super(message, "SHIPMENT_ALREADY_EXISTS");
    }

    public ShipmentAlreadyExistsException(Long sellerOrderId) {
        super("Shipment already exists for seller order ID: " + sellerOrderId, "SHIPMENT_ALREADY_EXISTS");
    }
}
