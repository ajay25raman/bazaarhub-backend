package com.bazaarhub.backend.wishlist.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class InvalidWishlistOperationException extends DomainException {
    public InvalidWishlistOperationException(String message) {
        super(message, "INVALID_WISHLIST_OPERATION");
    }
}
