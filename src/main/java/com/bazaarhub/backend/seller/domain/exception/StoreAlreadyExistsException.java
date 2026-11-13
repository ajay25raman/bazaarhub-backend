package com.bazaarhub.backend.seller.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class StoreAlreadyExistsException extends DomainException {

    public StoreAlreadyExistsException(String message) {
        super(message, "STORE_ALREADY_EXISTS");
    }
}
