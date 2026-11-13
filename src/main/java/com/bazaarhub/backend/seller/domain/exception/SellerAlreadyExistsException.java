package com.bazaarhub.backend.seller.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class SellerAlreadyExistsException extends DomainException {

    public SellerAlreadyExistsException(String message) {
        super(message, "SELLER_ALREADY_EXISTS");
    }
}
