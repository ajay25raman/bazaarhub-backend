package com.bazaarhub.backend.cart.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class CartNotFoundException extends ResourceNotFoundException {

    public CartNotFoundException(String message) {
        super(message);
    }

    public CartNotFoundException(Long cartId) {
        super("Cart", cartId);
    }
}
