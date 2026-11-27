package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring configuration for BazaarHub dual-mode rate limiting architecture.
 */
@Configuration
@EnableConfigurationProperties(RateLimitConfigProperties.class)
public class RateLimitingConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingConfig.class);

    @Bean
    public InMemoryRateLimiterService inMemoryRateLimiterService() {
        return new InMemoryRateLimiterService();
    }

    /**
     * Primary rate limiter service selection based on configured engine type.
     */
    @Bean
    @Primary
    public RateLimiterService rateLimiterService(
            RateLimitConfigProperties properties,
            InMemoryRateLimiterService inMemoryLimiter,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {

        String selectedType = properties.getType();
        if ("redis".equalsIgnoreCase(selectedType)) {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                log.error("[RATE LIMITER CONFIG ERROR] 'redis' engine requested but StringRedisTemplate is unavailable in context!");
                throw new IllegalStateException("Redis rate limiting is configured (bazaarhub.rate-limiting.type=redis), "
                        + "but no Redis connection or StringRedisTemplate could be established.");
            }
            log.info("[RATE LIMITER INITIALIZED] Active provider: REDIS (Distributed Token Bucket)");
            return new RedisRateLimiterService(redisTemplate, inMemoryLimiter);
        }

        log.info("[RATE LIMITER INITIALIZED] Active provider: IN_MEMORY (Local Development & Testing)");
        return inMemoryLimiter;
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter(
            RateLimiterService rateLimiterService,
            ClientIpResolver clientIpResolver,
            RateLimitConfigProperties properties,
            ObjectMapper objectMapper) {
        return new RateLimitingFilter(rateLimiterService, clientIpResolver, properties, objectMapper);
    }

    /**
     * Disables automatic Servlet container registration for RateLimitingFilter.
     * Guarantees that the filter is ONLY executed within the Spring Security FilterChain before JwtAuthenticationFilter,
     * preventing duplicate execution.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
