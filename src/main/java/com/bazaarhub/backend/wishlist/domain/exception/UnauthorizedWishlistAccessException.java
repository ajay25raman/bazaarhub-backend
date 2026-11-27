package com.bazaarhub.backend.wishlist.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedWishlistAccessException extends DomainException {
    public UnauthorizedWishlistAccessException(String message) {
        super(message, "UNAUTHORIZED_WISHLIST_ACCESS");
    }
}
