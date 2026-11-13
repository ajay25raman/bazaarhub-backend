package com.bazaarhub.backend.seller.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class SellerNotFoundException extends ResourceNotFoundException {

    public SellerNotFoundException(String identifier) {
        super("Seller", identifier);
    }

    public SellerNotFoundException(Long id) {
        super("Seller", id != null ? id.toString() : "null");
    }
}
