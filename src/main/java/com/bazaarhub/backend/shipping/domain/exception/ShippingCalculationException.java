package com.bazaarhub.backend.shipping.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class ShippingCalculationException extends DomainException {
    public ShippingCalculationException(String message) {
        super(message, "SHIPPING_CALCULATION_ERROR");
    }
}
