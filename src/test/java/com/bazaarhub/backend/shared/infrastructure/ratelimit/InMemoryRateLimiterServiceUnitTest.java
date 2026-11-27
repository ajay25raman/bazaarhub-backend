package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRateLimiterServiceUnitTest {

    private MutableTestClock testClock;
    private InMemoryRateLimiterService rateLimiter;
    private RateLimitRule loginRule;
    private RateLimitRule searchRule;

    @BeforeEach
    void setUp() {
        testClock = new MutableTestClock(Instant.parse("2026-11-18T10:00:00Z"));
        rateLimiter = new InMemoryRateLimiterService(testClock);

        loginRule = new RateLimitRule(
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
                10,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_OPEN_WITH_LOCAL_FALLBACK
        );
    }

    @Test
    @DisplayName("First request for a client key is allowed with remaining capacity")
    void testFirstRequestAllowed() {
        RateLimitResult result = rateLimiter.tryAcquire("192.168.1.100", loginRule);

        assertTrue(result.allowed());
        assertEquals(4, result.remainingTokens());
        assertEquals(0, result.retryAfterSeconds());
        assertFalse(result.isInfrastructureError());
    }

    @Test
    @DisplayName("Requests within configured capacity are allowed")
    void testRequestsWithinCapacityAllowed() {
        for (int i = 0; i < 5; i++) {
            RateLimitResult result = rateLimiter.tryAcquire("192.168.1.100", loginRule);
            assertTrue(result.allowed(), "Request " + (i + 1) + " should be allowed");
            assertEquals(4 - i, result.remainingTokens());
        }
    }

    @Test
    @DisplayName("Request beyond configured capacity is rejected with Retry-After seconds")
    void testRequestBeyondCapacityRejected() {
        // Exhaust 5 tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("192.168.1.100", loginRule);
        }

        // 6th request should be rejected
        RateLimitResult rejectedResult = rateLimiter.tryAcquire("192.168.1.100", loginRule);

        assertFalse(rejectedResult.allowed());
        assertEquals(0, rejectedResult.remainingTokens());
        assertTrue(rejectedResult.retryAfterSeconds() > 0, "Retry-After must be positive");
        assertTrue(rejectedResult.retryAfterSeconds() <= 60, "Retry-After should not exceed window duration");
    }

    @Test
    @DisplayName("Tokens refill predictably after elapsed time")
    void testTokenRefillAfterElapsedTime() {
        // Exhaust 5 tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("192.168.1.100", loginRule);
        }

        // Immediately rejected
        assertFalse(rateLimiter.tryAcquire("192.168.1.100", loginRule).allowed());

        // Advance clock by 36 seconds (refill rate is 5 tokens / 60s = 1 token per 12s -> 3 tokens refilled)
        testClock.advance(Duration.ofSeconds(36));

        // Next 3 requests should succeed
        assertTrue(rateLimiter.tryAcquire("192.168.1.100", loginRule).allowed());
        assertTrue(rateLimiter.tryAcquire("192.168.1.100", loginRule).allowed());
        assertTrue(rateLimiter.tryAcquire("192.168.1.100", loginRule).allowed());

        // 4th request exceeds refilled capacity
        assertFalse(rateLimiter.tryAcquire("192.168.1.100", loginRule).allowed());
    }

    @Test
    @DisplayName("Different client keys receive independent rate limit buckets")
    void testIndependentClientKeys() {
        String clientA = "10.0.0.1";
        String clientB = "10.0.0.2";

        // Exhaust client A
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(clientA, loginRule).allowed());
        }
        assertFalse(rateLimiter.tryAcquire(clientA, loginRule).allowed());

        // Client B should still have full capacity
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(clientB, loginRule).allowed());
        }
        assertFalse(rateLimiter.tryAcquire(clientB, loginRule).allowed());
    }

    @Test
    @DisplayName("Different route keys receive independent rate limit buckets for the same client")
    void testIndependentRouteKeys() {
        String client = "10.0.0.1";

        // Exhaust login route
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(client, loginRule).allowed());
        }
        assertFalse(rateLimiter.tryAcquire(client, loginRule).allowed());

        // Search route should be completely unaffected
        assertTrue(rateLimiter.tryAcquire(client, searchRule).allowed());
    }

    @Test
    @DisplayName("Concurrent requests cannot exceed configured capacity")
    void testConcurrentRequestsCannotExceedCapacity() throws InterruptedException {
        int threadCount = 20;
        int capacity = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    RateLimitResult res = rateLimiter.tryAcquire("10.10.10.10", loginRule);
                    if (res.allowed()) {
                        allowedCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Fire all threads simultaneously
        doneLatch.await();
        executor.shutdown();

        assertEquals(capacity, allowedCount.get(), "Exactly capacity (5) tokens should be acquired concurrently");
        assertEquals(threadCount - capacity, rejectedCount.get(), "Remaining 15 concurrent attempts must be rejected");
    }

    @Test
    @DisplayName("Stale entries are purged during expired bucket cleanup")
    void testExpiredEntriesCleanup() {
        rateLimiter.tryAcquire("10.0.0.1", loginRule);
        rateLimiter.tryAcquire("10.0.0.2", loginRule);
        rateLimiter.tryAcquire("10.0.0.3", searchRule);

        assertEquals(3, rateLimiter.getActiveBucketCount());

        // Advance time past idle expiration
        testClock.advance(Duration.ofMinutes(10));

        // Access only client 1
        rateLimiter.tryAcquire("10.0.0.1", loginRule);

        // Cleanup buckets idle for > 5 minutes
        int evicted = rateLimiter.cleanupExpiredBuckets(Duration.ofMinutes(5));

        assertEquals(2, evicted, "Clients 2 and 3 should be evicted");
        assertEquals(1, rateLimiter.getActiveBucketCount(), "Only client 1 should remain active");
    }

    @Test
    @DisplayName("Reset clears rate limit bucket for specific client and route")
    void testReset() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("10.0.0.1", loginRule);
        }
        assertFalse(rateLimiter.tryAcquire("10.0.0.1", loginRule).allowed());

        rateLimiter.reset("10.0.0.1", loginRule);

        // Capacity should be restored
        assertTrue(rateLimiter.tryAcquire("10.0.0.1", loginRule).allowed());
    }

    /**
     * Mutable clock helper for deterministic time progression testing.
     */
    private static class MutableTestClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        MutableTestClock(Instant start) {
            this.currentInstant = start;
            this.zone = ZoneId.of("UTC");
        }

        void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableTestClock(currentInstant);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
