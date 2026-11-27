package com.bazaarhub.backend.wishlist.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class WishlistItemAlreadyExistsException extends DomainException {
    public WishlistItemAlreadyExistsException(String message) {
        super(message, "WISHLIST_ITEM_ALREADY_EXISTS");
    }

    public WishlistItemAlreadyExistsException(Long wishlistId, Long productId) {
        super(String.format("Product ID %d is already in wishlist ID %d", productId, wishlistId), "WISHLIST_ITEM_ALREADY_EXISTS");
    }
}
