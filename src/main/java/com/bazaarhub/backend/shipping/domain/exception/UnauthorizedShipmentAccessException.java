package com.bazaarhub.backend.shipping.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedShipmentAccessException extends DomainException {
    public UnauthorizedShipmentAccessException(String message) {
        super(message, "UNAUTHORIZED_SHIPMENT_ACCESS");
    }
}
