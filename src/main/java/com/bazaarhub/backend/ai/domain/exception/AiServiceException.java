package com.bazaarhub.backend.ai.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class AiServiceException extends DomainException {
    public AiServiceException(String message) {
        super(message, "AI_SERVICE_ERROR");
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, "AI_SERVICE_ERROR", cause);
    }
}
