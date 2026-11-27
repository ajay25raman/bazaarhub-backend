package com.bazaarhub.backend.shared.infrastructure.ratelimit;

/**
 * Value object representing the outcome of a rate-limiting evaluation.
 *
 * @param allowed               Whether the request is permitted to proceed
 * @param remainingTokens       Number of remaining tokens in the bucket for this window
 * @param retryAfterSeconds     Seconds the client must wait before retrying (if denied)
 * @param isFallback            Whether the result was evaluated using local fallback mode
 * @param isInfrastructureError Whether an underlying infrastructure error (e.g. Redis failure) occurred
 */
public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds,
        boolean isFallback,
        boolean isInfrastructureError
) {
    public static RateLimitResult allowed(long remainingTokens) {
        return new RateLimitResult(true, Math.max(0, remainingTokens), 0, false, false);
    }

    public static RateLimitResult allowedFallback(long remainingTokens) {
        return new RateLimitResult(true, Math.max(0, remainingTokens), 0, true, false);
    }

    public static RateLimitResult denied(long retryAfterSeconds) {
        return new RateLimitResult(false, 0, Math.max(1, retryAfterSeconds), false, false);
    }

    public static RateLimitResult deniedFallback(long retryAfterSeconds) {
        return new RateLimitResult(false, 0, Math.max(1, retryAfterSeconds), true, false);
    }

    public static RateLimitResult infrastructureError() {
        return new RateLimitResult(false, 0, 0, false, true);
    }
}
