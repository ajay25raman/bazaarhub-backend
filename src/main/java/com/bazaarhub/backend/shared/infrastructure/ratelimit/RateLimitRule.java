package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import org.springframework.http.HttpMethod;

import java.time.Duration;

/**
 * Definition of a rate limiting policy rule for a specific route.
 *
 * @param routeName       Unique identifier for the route group (used in bucket keys)
 * @param httpMethod      Required HTTP method (e.g. POST, GET)
 * @param pathPattern     Ant-style path pattern (e.g. /api/v1/auth/login, /api/v1/discovery/**)
 * @param capacity        Maximum request tokens allowed within the window
 * @param window          Duration of the rate limit window
 * @param failureStrategy Strategy to adopt if distributed storage (Redis) is unavailable
 */
public record RateLimitRule(
        String routeName,
        HttpMethod httpMethod,
        String pathPattern,
        int capacity,
        Duration window,
        FailureStrategy failureStrategy
) {
    public enum FailureStrategy {
        /**
         * Safely reject requests with HTTP 503 on infrastructure failure (recommended for auth & AI).
         */
        FAIL_CLOSED,

        /**
         * Temporarily fallback to node-local in-memory limiter for eligible public read routes.
         */
        FAIL_OPEN_WITH_LOCAL_FALLBACK
    }

    /**
     * Calculates the token refill rate per millisecond.
     */
    public double getRefillRatePerMs() {
        return (double) capacity / window.toMillis();
    }
}
