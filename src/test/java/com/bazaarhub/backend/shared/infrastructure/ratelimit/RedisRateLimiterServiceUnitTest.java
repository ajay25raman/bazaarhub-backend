package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private InMemoryRateLimiterService localFallbackLimiter;
    private RedisRateLimiterService redisRateLimiter;

    private RateLimitRule authLoginRule;
    private RateLimitRule searchRule;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneId.of("UTC"));
        localFallbackLimiter = new InMemoryRateLimiterService(fixedClock);
        redisRateLimiter = new RedisRateLimiterService(redisTemplate, localFallbackLimiter, fixedClock);

        authLoginRule = new RateLimitRule(
                "auth-login",
                HttpMethod.POST,
                "/api/v1/auth/login",
                5,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_CLOSED
        );

        searchRule = new RateLimitRule(
                "public-search",
                HttpMethod.GET,
                "/api/v1/search/**",
                60,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_OPEN_WITH_LOCAL_FALLBACK
        );
    }

    @Test
    @DisplayName("Redis allowed response returns allowed Result with remaining tokens")
    void testRedisAllowedResponse() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("bazaarhub:rate-limit:auth-login:192.168.1.1")),
                eq("5"),
                eq("60000"),
                anyString(),
                anyString()
        )).thenReturn(List.of(1L, 4L, 0L));

        RateLimitResult result = redisRateLimiter.tryAcquire("192.168.1.1", authLoginRule);

        assertTrue(result.allowed());
        assertEquals(4, result.remainingTokens());
        assertEquals(0, result.retryAfterSeconds());
        assertFalse(result.isFallback());
        assertFalse(result.isInfrastructureError());
    }

    @Test
    @DisplayName("Redis denied response returns denied Result with retryAfter seconds")
    void testRedisDeniedResponse() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("bazaarhub:rate-limit:auth-login:192.168.1.1")),
                any(), any(), any(), any()
        )).thenReturn(List.of(0L, 0L, 12L));

        RateLimitResult result = redisRateLimiter.tryAcquire("192.168.1.1", authLoginRule);

        assertFalse(result.allowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(12, result.retryAfterSeconds());
        assertFalse(result.isFallback());
        assertFalse(result.isInfrastructureError());
    }

    @Test
    @DisplayName("Redis connection failure triggers FAIL_CLOSED with infrastructure error on auth routes")
    void testRedisConnectionFailureFailClosed() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("Connection refused to srv-redis:6379"));

        RateLimitResult result = redisRateLimiter.tryAcquire("192.168.1.1", authLoginRule);

        assertFalse(result.allowed());
        assertTrue(result.isInfrastructureError(), "Auth route must trigger infrastructure error on Redis failure");
        assertFalse(result.isFallback());
    }

    @Test
    @DisplayName("Redis query timeout triggers FAIL_OPEN_WITH_LOCAL_FALLBACK on eligible public routes")
    void testRedisTimeoutTriggersLocalFallback() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any()))
                .thenThrow(new QueryTimeoutException("Redis command timed out after 2000ms"));

        RateLimitResult result = redisRateLimiter.tryAcquire("192.168.1.1", searchRule);

        assertTrue(result.allowed(), "Search route must successfully fall back to local in-memory limiter");
        assertTrue(result.isFallback(), "Result must be flagged as evaluated via fallback mode");
        assertFalse(result.isInfrastructureError());
    }

    @Test
    @DisplayName("Reset executes Redis DELETE on key")
    void testResetExecutesDelete() {
        redisRateLimiter.reset("192.168.1.1", authLoginRule);

        verify(redisTemplate, times(1)).delete("bazaarhub:rate-limit:auth-login:192.168.1.1");
    }

    @Test
    @DisplayName("Provider type is reported as REDIS")
    void testProviderType() {
        assertEquals("REDIS", redisRateLimiter.getProviderType());
    }
}
