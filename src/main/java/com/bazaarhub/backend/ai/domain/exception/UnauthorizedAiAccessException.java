package com.bazaarhub.backend.ai.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class UnauthorizedAiAccessException extends DomainException {
    public UnauthorizedAiAccessException(String message) {
        super(message, "UNAUTHORIZED_AI_ACCESS");
    }
}
