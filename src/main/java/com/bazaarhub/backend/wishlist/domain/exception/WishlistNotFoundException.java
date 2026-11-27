package com.bazaarhub.backend.wishlist.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class WishlistNotFoundException extends ResourceNotFoundException {
    public WishlistNotFoundException(String message) {
        super(message);
    }

    public WishlistNotFoundException(Long wishlistId) {
        super(String.format("Wishlist with identifier '%d' was not found", wishlistId));
    }
}
