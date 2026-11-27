package com.bazaarhub.backend.shipping.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class IneligibleOrderForShipmentException extends DomainException {
    public IneligibleOrderForShipmentException(String message) {
        super(message, "INELIGIBLE_ORDER_FOR_SHIPMENT");
    }
}
