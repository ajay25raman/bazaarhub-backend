package com.bazaarhub.backend.seller.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class StoreNotFoundException extends ResourceNotFoundException {

    public StoreNotFoundException(String identifier) {
        super("Store", identifier);
    }

    public StoreNotFoundException(Long id) {
        super("Store", id != null ? id.toString() : "null");
    }
}
