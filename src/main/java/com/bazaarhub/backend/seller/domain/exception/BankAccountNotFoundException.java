package com.bazaarhub.backend.seller.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class BankAccountNotFoundException extends ResourceNotFoundException {

    public BankAccountNotFoundException(String identifier) {
        super("BankAccount", identifier);
    }

    public BankAccountNotFoundException(Long id) {
        super("BankAccount", id != null ? id.toString() : "null");
    }
}
