package com.bazaarhub.backend.ai.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class AiDisabledException extends DomainException {
    public AiDisabledException(String message) {
        super(message, "AI_DISABLED");
    }
}
