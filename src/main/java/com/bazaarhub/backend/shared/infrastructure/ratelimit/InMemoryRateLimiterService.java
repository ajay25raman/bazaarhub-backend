package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory token bucket rate limiter implementation.
 *
 * <p>Designed for local development, standalone deployments, and deterministic automated testing.
 * Uses atomic per-bucket synchronization and time-based token refills.
 *
 * <p>NOTE: This provider does NOT provide distributed rate limiting across horizontal multi-instance clusters.
 */
public class InMemoryRateLimiterService implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimiterService.class);

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiterService() {
        this(Clock.systemUTC());
    }

    public InMemoryRateLimiterService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RateLimitResult tryAcquire(String clientKey, RateLimitRule rule) {
        String bucketKey = buildBucketKey(rule.routeName(), clientKey);
        long nowMs = clock.millis();

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(rule.capacity(), nowMs));
        RateLimitResult result = bucket.tryAcquire(rule, nowMs);

        if (!result.allowed()) {
            log.warn("[RATE LIMIT EXCEEDED] Route '{}' denied for client '{}'. Retry after: {}s",
                    rule.routeName(), clientKey, result.retryAfterSeconds());
        }

        return result;
    }

    @Override
    public void reset(String clientKey, RateLimitRule rule) {
        String bucketKey = buildBucketKey(rule.routeName(), clientKey);
        buckets.remove(bucketKey);
    }

    /**
     * Clears all in-memory buckets (used to isolate test scenarios).
     */
    public void clearAll() {
        buckets.clear();
    }

    @Override
    public String getProviderType() {
        return "IN_MEMORY";
    }

    /**
     * Purges stale token buckets that have not been accessed within the max idle duration.
     * Prevents unbounded heap memory growth from high-cardinality transient client IPs.
     *
     * @param maxIdleDuration Maximum idle duration before bucket is evicted
     * @return Number of evicted buckets
     */
    public int cleanupExpiredBuckets(Duration maxIdleDuration) {
        long nowMs = clock.millis();
        long maxIdleMs = maxIdleDuration.toMillis();
        int initialSize = buckets.size();

        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(nowMs, maxIdleMs));

        int evicted = initialSize - buckets.size();
        if (evicted > 0) {
            log.info("[RATE LIMITER CLEANUP] Evicted {} expired in-memory buckets. Active buckets: {}",
                    evicted, buckets.size());
        }
        return evicted;
    }

    /**
     * Returns total active bucket count (for metrics and test assertions).
     */
    public int getActiveBucketCount() {
        return buckets.size();
    }

    private String buildBucketKey(String routeName, String clientKey) {
        return routeName + ":" + clientKey;
    }

    /**
     * Internal stateful token bucket with localized synchronization.
     */
    private static final class TokenBucket {
        private double tokens;
        private long lastRefillMs;
        private long lastAccessedMs;

        private TokenBucket(int capacity, long nowMs) {
            this.tokens = capacity;
            this.lastRefillMs = nowMs;
            this.lastAccessedMs = nowMs;
        }

        private synchronized RateLimitResult tryAcquire(RateLimitRule rule, long nowMs) {
            this.lastAccessedMs = nowMs;

            // Refill tokens based on elapsed wall-clock time
            long elapsedMs = Math.max(0, nowMs - lastRefillMs);
            double refilled = elapsedMs * rule.getRefillRatePerMs();
            this.tokens = Math.min(rule.capacity(), this.tokens + refilled);
            this.lastRefillMs = nowMs;

            if (this.tokens >= 1.0) {
                this.tokens -= 1.0;
                return RateLimitResult.allowed((long) Math.floor(this.tokens));
            } else {
                double deficit = 1.0 - this.tokens;
                long retryMs = (long) Math.ceil(deficit / rule.getRefillRatePerMs());
                long retrySec = Math.max(1, (long) Math.ceil((double) retryMs / 1000.0));
                return RateLimitResult.denied(retrySec);
            }
        }

        private synchronized boolean isExpired(long nowMs, long maxIdleMs) {
            return (nowMs - lastAccessedMs) > maxIdleMs;
        }
    }
}
