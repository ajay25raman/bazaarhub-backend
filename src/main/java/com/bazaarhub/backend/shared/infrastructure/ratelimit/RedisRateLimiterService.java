package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Clock;
import java.util.Collections;
import java.util.List;

/**
 * Production Redis-backed distributed token bucket rate limiter implementation.
 *
 * <p>Executes an atomic server-side Lua script on the Redis cluster to prevent race conditions
 * across horizontally scaled application instances. Sets explicit TTLs on keys to prevent unbounded memory growth.
 *
 * <p>Implements secure failure strategies (FAIL_CLOSED for auth/AI routes, explicit fallback for public read routes).
 */
public class RedisRateLimiterService implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterService.class);

    private static final String KEY_PREFIX = "bazaarhub:rate-limit:";

    private static final String LUA_TOKEN_BUCKET_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local window_ms = tonumber(ARGV[2])
            local now_ms = tonumber(ARGV[3])
            local ttl_sec = tonumber(ARGV[4])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])

            if tokens == nil or last_refill == nil then
                tokens = capacity
                last_refill = now_ms
            else
                local elapsed = math.max(0, now_ms - last_refill)
                local refill = (elapsed * capacity) / window_ms
                tokens = math.min(capacity, tokens + refill)
                last_refill = now_ms
            end

            if tokens >= 1 then
                tokens = tokens - 1
                redis.call('HMSET', key, 'tokens', tostring(tokens), 'last_refill', tostring(last_refill))
                redis.call('EXPIRE', key, ttl_sec)
                return {1, math.floor(tokens), 0}
            else
                local needed = 1 - tokens
                local retry_ms = math.ceil((needed * window_ms) / capacity)
                local retry_sec = math.max(1, math.ceil(retry_ms / 1000))
                redis.call('HMSET', key, 'tokens', tostring(tokens), 'last_refill', tostring(last_refill))
                redis.call('EXPIRE', key, ttl_sec)
                return {0, 0, retry_sec}
            end
            """;

    private final StringRedisTemplate redisTemplate;
    private final InMemoryRateLimiterService localFallbackLimiter;
    private final DefaultRedisScript<List> redisScript;
    private final Clock clock;

    public RedisRateLimiterService(StringRedisTemplate redisTemplate,
                                   InMemoryRateLimiterService localFallbackLimiter) {
        this(redisTemplate, localFallbackLimiter, Clock.systemUTC());
    }

    public RedisRateLimiterService(StringRedisTemplate redisTemplate,
                                   InMemoryRateLimiterService localFallbackLimiter,
                                   Clock clock) {
        this.redisTemplate = redisTemplate;
        this.localFallbackLimiter = localFallbackLimiter;
        this.clock = clock;

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_TOKEN_BUCKET_SCRIPT);
        script.setResultType(List.class);
        this.redisScript = script;
    }

    @Override
    public RateLimitResult tryAcquire(String clientKey, RateLimitRule rule) {
        String key = buildRedisKey(rule.routeName(), clientKey);
        long nowMs = clock.millis();
        long windowMs = rule.window().toMillis();
        // TTL is set to twice the window duration to allow stale buckets to expire naturally
        long ttlSec = Math.max(60, rule.window().toSeconds() * 2);

        try {
            List<?> rawResult = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    String.valueOf(rule.capacity()),
                    String.valueOf(windowMs),
                    String.valueOf(nowMs),
                    String.valueOf(ttlSec)
            );

            if (rawResult != null && rawResult.size() >= 3) {
                long allowedCode = ((Number) rawResult.get(0)).longValue();
                long remaining = ((Number) rawResult.get(1)).longValue();
                long retryAfter = ((Number) rawResult.get(2)).longValue();

                if (allowedCode == 1) {
                    return RateLimitResult.allowed(remaining);
                } else {
                    log.warn("[REDIS RATE LIMIT EXCEEDED] Route '{}' denied for client '{}'. Retry after: {}s",
                            rule.routeName(), clientKey, retryAfter);
                    return RateLimitResult.denied(retryAfter);
                }
            }

            log.error("[RATE LIMITER REDIS UNEXPECTED RESULT] Empty response from Lua script for key: {}", key);
            return handleRedisFailure(clientKey, rule);

        } catch (Exception ex) {
            log.error("[RATE LIMITER REDIS FAILURE] Route '{}' client '{}': Redis operation failed: {}",
                    rule.routeName(), clientKey, ex.getClass().getSimpleName());
            return handleRedisFailure(clientKey, rule);
        }
    }

    @Override
    public void reset(String clientKey, RateLimitRule rule) {
        String key = buildRedisKey(rule.routeName(), clientKey);
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("[RATE LIMITER REDIS RESET FAILED] Could not delete key '{}': {}", key, ex.getMessage());
        }
    }

    @Override
    public String getProviderType() {
        return "REDIS";
    }

    private RateLimitResult handleRedisFailure(String clientKey, RateLimitRule rule) {
        if (rule.failureStrategy() == RateLimitRule.FailureStrategy.FAIL_OPEN_WITH_LOCAL_FALLBACK
                && localFallbackLimiter != null) {
            log.warn("[RATE LIMITER DEGRADED] Executing local in-memory fallback for route '{}' and client '{}'",
                    rule.routeName(), clientKey);
            RateLimitResult localResult = localFallbackLimiter.tryAcquire(clientKey, rule);
            return localResult.allowed()
                    ? RateLimitResult.allowedFallback(localResult.remainingTokens())
                    : RateLimitResult.deniedFallback(localResult.retryAfterSeconds());
        }

        // FAIL_CLOSED: Return explicit infrastructure error
        return RateLimitResult.infrastructureError();
    }

    private String buildRedisKey(String routeName, String clientKey) {
        return KEY_PREFIX + routeName + ":" + clientKey;
    }
}
