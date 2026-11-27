package com.bazaarhub.backend.ai.domain.exception;

import com.bazaarhub.backend.shared.domain.exception.DomainException;

public class AiRateLimitException extends DomainException {
    private final Long retryAfterSeconds;

    public AiRateLimitException(String message, Long retryAfterSeconds) {
        super(message, "AI_RATE_LIMIT");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
