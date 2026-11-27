package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Perimeter rate-limiting filter registered prior to {@link com.bazaarhub.backend.identity.infrastructure.security.JwtAuthenticationFilter}.
 *
 * <p>Enforces configurable token-bucket policies across sensitive authentication, search, and AI routes.
 * Emits HTTP 429 Too Many Requests with standardized {@link ApiResponse} payloads and Retry-After headers.
 * Emits HTTP 503 Service Unavailable when a critical infrastructure failure occurs in FAIL_CLOSED mode.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitConfigProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitingFilter(RateLimiterService rateLimiterService,
                              ClientIpResolver clientIpResolver,
                              RateLimitConfigProperties properties,
                              ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.clientIpResolver = clientIpResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }

        // Bypasses CORS preflight requests
        if (HttpMethod.OPTIONS.name().equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // If request does not match any configured rate-limit rule, pass through
        return findMatchingRule(request) == null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        RateLimitRule rule = findMatchingRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolveClientIp(request);
        RateLimitResult result = rateLimiterService.tryAcquire(clientIp, rule);

        // 1. Handle Infrastructure Failure (Redis outage in FAIL_CLOSED mode)
        if (result.isInfrastructureError()) {
            log.error("[RATE LIMITER FAIL_CLOSED] Rejecting request to '{}' from client '{}' due to infrastructure outage",
                    request.getRequestURI(), clientIp);
            handleServiceUnavailable(response);
            return;
        }

        // 2. Handle Rate Limit Exceeded
        if (!result.allowed()) {
            log.warn("[RATE LIMIT 429] Route '{}' limit exceeded for client '{}'. Retry after: {}s",
                    rule.routeName(), clientIp, result.retryAfterSeconds());
            handleRateLimitExceeded(response, rule, result);
            return;
        }

        // 3. Request Allowed: Populate informative telemetry headers and proceed
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));

        filterChain.doFilter(request, response);
    }

    private RateLimitRule findMatchingRule(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        for (RateLimitRule rule : properties.getDefaultRules()) {
            if (rule.httpMethod().name().equalsIgnoreCase(method)) {
                String pattern = rule.pathPattern();
                if (pattern.endsWith("/**")) {
                    String base = pattern.substring(0, pattern.length() - 3);
                    if (uri.equals(base) || uri.equals(base + "/") || pathMatcher.match(pattern, uri)) {
                        return rule;
                    }
                } else if (pathMatcher.match(pattern, uri)) {
                    return rule;
                }
            }
        }
        return null;
    }

    private void handleRateLimitExceeded(HttpServletResponse response,
                                         RateLimitRule rule,
                                         RateLimitResult result) throws IOException {
        response.setStatus(429); // HttpStatus.TOO_MANY_REQUESTS
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
        response.setHeader("X-RateLimit-Remaining", "0");

        String message = String.format("Too Many Requests. Rate limit exceeded for this endpoint. Please retry in %d seconds.",
                result.retryAfterSeconds());
        String errorDetail = String.format("Rate limit policy '%s': maximum %d requests per %s",
                rule.routeName(), rule.capacity(), rule.window());

        ApiResponse<Void> apiResponse = ApiResponse.error(message, List.of(errorDetail));
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private void handleServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); // 503
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(
                "Service Temporarily Unavailable: Rate limiting infrastructure is undergoing maintenance. Please retry shortly.",
                List.of("Distributed rate limiter unavailable")
        );
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
