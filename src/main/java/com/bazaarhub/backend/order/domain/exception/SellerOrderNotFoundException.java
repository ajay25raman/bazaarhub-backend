package com.bazaarhub.backend.order.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class SellerOrderNotFoundException extends ResourceNotFoundException {

    public SellerOrderNotFoundException(String message) {
        super(message);
    }

    public SellerOrderNotFoundException(Long sellerOrderId) {
        super("SellerOrder", sellerOrderId);
    }

    public SellerOrderNotFoundException(String fieldName, Object fieldValue) {
        super(String.format("SellerOrder with %s '%s' was not found", fieldName, fieldValue));
    }
}
