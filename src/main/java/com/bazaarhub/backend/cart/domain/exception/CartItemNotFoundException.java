package com.bazaarhub.backend.cart.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class CartItemNotFoundException extends ResourceNotFoundException {

    public CartItemNotFoundException(String message) {
        super(message);
    }

    public CartItemNotFoundException(Long itemId) {
        super("CartItem", itemId);
    }
}
