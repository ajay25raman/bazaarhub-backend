package com.bazaarhub.backend.inventory.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class ReservationNotFoundException extends ResourceNotFoundException {

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
