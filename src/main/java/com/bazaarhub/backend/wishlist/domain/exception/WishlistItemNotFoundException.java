package com.bazaarhub.backend.wishlist.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;

public class WishlistItemNotFoundException extends ResourceNotFoundException {
    public WishlistItemNotFoundException(String message) {
        super(message);
    }

    public WishlistItemNotFoundException(Long wishlistId, Long productId) {
        super(String.format("Product ID %d was not found in wishlist ID %d", productId, wishlistId));
    }
}
