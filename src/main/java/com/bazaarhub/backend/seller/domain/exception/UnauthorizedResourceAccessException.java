package com.bazaarhub.backend.seller.domain.exception;

import org.springframework.security.access.AccessDeniedException;

public class UnauthorizedResourceAccessException extends AccessDeniedException {

    public UnauthorizedResourceAccessException(String message) {
        super(message);
    }
}
