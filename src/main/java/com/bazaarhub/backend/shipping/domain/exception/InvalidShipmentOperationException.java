package com.bazaarhub.backend.shipping.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidShipmentOperationException extends DomainException {
    public InvalidShipmentOperationException(String message) {
        super(message, "INVALID_SHIPMENT_OPERATION");
    }
}
