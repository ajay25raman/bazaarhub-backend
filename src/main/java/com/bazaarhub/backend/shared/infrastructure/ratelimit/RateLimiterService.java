package com.bazaarhub.backend.shared.infrastructure.ratelimit;

/**
 * Unified rate-limiting service abstraction supporting dual-mode engines:
 * Redis-backed distributed token bucket for production and in-memory token bucket for local testing.
 */
public interface RateLimiterService {

    /**
     * Atomically checks whether a request is allowed and consumes one token if permitted.
     *
     * @param clientKey Unique client identity (e.g. validated client IP address)
     * @param rule      Configured rate-limiting policy for the targeted route
     * @return Result containing permit status, remaining tokens, retry-after duration, and flags
     */
    RateLimitResult tryAcquire(String clientKey, RateLimitRule rule);

    /**
     * Resets the rate limit bucket for a client and route (used for testing and admin overrides).
     *
     * @param clientKey Unique client identity
     * @param rule      Configured rate-limiting policy
     */
    void reset(String clientKey, RateLimitRule rule);

    /**
     * Returns the active provider type name (e.g. "REDIS" or "IN_MEMORY").
     */
    String getProviderType();
}
