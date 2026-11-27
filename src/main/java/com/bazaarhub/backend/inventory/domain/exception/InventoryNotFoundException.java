package com.bazaarhub.backend.inventory.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class InventoryNotFoundException extends ResourceNotFoundException {

    public InventoryNotFoundException(String message) {
        super(message);
    }
}
