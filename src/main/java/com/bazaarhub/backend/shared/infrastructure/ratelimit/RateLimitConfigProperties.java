package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralized configuration properties for BazaarHub rate limiting.
 */
@Data
@ConfigurationProperties(prefix = "bazaarhub.rate-limiting")
public class RateLimitConfigProperties {

    /**
     * Whether rate limiting is globally enabled.
     */
    private boolean enabled = true;

    /**
     * Rate limiter engine type: "in-memory" or "redis".
     */
    private String type = "in-memory";

    /**
     * Comma-separated list of trusted reverse proxy IP addresses.
     */
    private String trustedProxies = "127.0.0.1,::1,0:0:0:0:0:0:0:1";

    /**
     * Global default Redis failure strategy: "FAIL_CLOSED" or "FAIL_OPEN_WITH_LOCAL_FALLBACK".
     */
    private String redisFailureStrategy = "FAIL_CLOSED";

    /**
     * Returns parsed set of trusted proxy IP addresses.
     */
    public Set<String> getTrustedProxySet() {
        if (trustedProxies == null || trustedProxies.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Configured rate limit rules for target endpoint groups.
     */
    public List<RateLimitRule> getDefaultRules() {
        List<RateLimitRule> rules = new ArrayList<>();

        // 1. POST /api/v1/auth/login — 5 requests / 1 minute (Brute force & credential stuffing protection)
        rules.add(new RateLimitRule(
                "auth-login",
                HttpMethod.POST,
                "/api/v1/auth/login",
                5,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_CLOSED
        ));

        // 2. POST /api/v1/auth/register — 3 requests / 5 minutes (Spam bot registration protection)
        rules.add(new RateLimitRule(
                "auth-register",
                HttpMethod.POST,
                "/api/v1/auth/register",
                3,
                Duration.ofMinutes(5),
                RateLimitRule.FailureStrategy.FAIL_CLOSED
        ));

        // 3. POST /api/v1/auth/forgot-password — 3 requests / 15 minutes (Email enumeration & OTP spam protection)
        rules.add(new RateLimitRule(
                "auth-forgot-password",
                HttpMethod.POST,
                "/api/v1/auth/forgot-password",
                3,
                Duration.ofMinutes(15),
                RateLimitRule.FailureStrategy.FAIL_CLOSED
        ));

        // 4. GET /api/v1/search/** — 60 requests / 1 minute (Full-text search query DoS protection)
        rules.add(new RateLimitRule(
                "public-search",
                HttpMethod.GET,
                "/api/v1/search/**",
                60,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_OPEN_WITH_LOCAL_FALLBACK
        ));

        // 5. GET /api/v1/discovery/** — 90 requests / 1 minute (Catalog scraping & cache thrashing protection)
        rules.add(new RateLimitRule(
                "public-discovery",
                HttpMethod.GET,
                "/api/v1/discovery/**",
                90,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_OPEN_WITH_LOCAL_FALLBACK
        ));

        // 6. POST /api/v1/shipments/rate-quote — 20 requests / 1 minute (Shipping API quota burn protection)
        rules.add(new RateLimitRule(
                "shipping-rate-quote",
                HttpMethod.POST,
                "/api/v1/shipments/rate-quote",
                20,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_CLOSED
        ));

        // 7. POST /api/v1/public/ai/** — 10 requests / 1 minute (Groq AI token budget exhaustion protection)
        rules.add(new RateLimitRule(
                "public-ai",
                HttpMethod.POST,
                "/api/v1/public/ai/**",
                10,
                Duration.ofMinutes(1),
                RateLimitRule.FailureStrategy.FAIL_CLOSED
        ));

        return rules;
    }
}
