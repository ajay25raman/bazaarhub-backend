package com.bazaarhub.backend.ai.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class AiTimeoutException extends DomainException {
    public AiTimeoutException(String message) {
        super(message, "AI_TIMEOUT");
    }
}
