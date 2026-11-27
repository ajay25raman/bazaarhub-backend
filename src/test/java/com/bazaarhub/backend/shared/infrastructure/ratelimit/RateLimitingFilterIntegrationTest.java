package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for perimeter RateLimitingFilter.
 * Verifies route-specific token bucket enforcement, header injection, spoofing protection, and CORS preflight bypass.
 */
@SpringBootTest(properties = "bazaarhub.rate-limiting.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitingFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryRateLimiterService inMemoryLimiter;

    @BeforeEach
    void setUp() {
        // Reset in-memory rate limiter state to ensure complete scenario isolation
        inMemoryLimiter.clearAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login allows exactly 5 attempts then returns HTTP 429 on 6th attempt")
    void testAuthLoginRateLimit() throws Exception {
        LoginRequest loginPayload = LoginRequest.builder()
                .identifier("test.ratelimit@bazaarhub.com")
                .password("TestPassword123!")
                .build();

        String json = objectMapper.writeValueAsString(loginPayload);
        String clientIp = "172.16.0.10";

        // First 5 requests must NOT return 429
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().is(not(429)))
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        // 6th attempt MUST return HTTP 429 Too Many Requests with Retry-After header
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Too Many Requests")))
                .andExpect(jsonPath("$.errors", hasItem(containsString("auth-login"))));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register allows 3 attempts then returns HTTP 429 on 4th attempt")
    void testAuthRegisterRateLimit() throws Exception {
        RegisterRequest registerPayload = RegisterRequest.builder()
                .fullName("Test Customer")
                .email("register.rate@bazaarhub.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .phone("+919876543210")
                .build();

        String json = objectMapper.writeValueAsString(registerPayload);
        String clientIp = "172.16.0.20";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().is(not(429)));
        }

        // 4th request must be rate-limited
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password allows 3 attempts then returns HTTP 429 on 4th attempt")
    void testForgotPasswordRateLimit() throws Exception {
        String clientIp = "172.16.0.30";
        String payload = "{\"email\":\"forgot.test@bazaarhub.com\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().is(not(429)));
        }

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message", containsString("Too Many Requests")));
    }

    @Test
    @DisplayName("GET /api/v1/search enforces 60 requests per minute limit")
    void testSearchRateLimit() throws Exception {
        String clientIp = "172.16.0.40";

        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/v1/search")
                            .param("keyword", "phone")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            }))
                    .andExpect(status().is(not(429)));
        }

        // 61st request must be rate limited
        mockMvc.perform(get("/api/v1/search")
                        .param("keyword", "phone")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        }))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("GET /api/v1/discovery/** enforces 90 requests per minute limit")
    void testDiscoveryRateLimit() throws Exception {
        String clientIp = "172.16.0.50";

        for (int i = 0; i < 90; i++) {
            mockMvc.perform(get("/api/v1/discovery/featured")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            }))
                    .andExpect(status().is(not(429)));
        }

        // 91st request must be rate limited
        mockMvc.perform(get("/api/v1/discovery/featured")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        }))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("POST /api/v1/shipments/rate-quote enforces 20 requests per minute limit")
    void testShippingRateQuoteRateLimit() throws Exception {
        String clientIp = "172.16.0.60";
        String payload = "{\"originPincode\":\"560001\",\"destinationPincode\":\"110001\",\"weightKg\":1.5}";

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/shipments/rate-quote")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().is(not(429)));
        }

        // 21st request must be rate limited
        mockMvc.perform(post("/api/v1/shipments/rate-quote")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("POST /api/v1/public/ai/** enforces 10 requests per minute limit")
    void testPublicAiRateLimit() throws Exception {
        String clientIp = "172.16.0.70";
        String payload = "{\"message\":\"Recommend a laptop\"}";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/public/ai/guest-chat")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().is(not(429)));
        }

        // 11th request must be rate limited
        mockMvc.perform(post("/api/v1/public/ai/guest-chat")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("OPTIONS preflight requests bypass rate limiting without decrementing quota")
    void testOptionsPreflightBypassesRateLimiting() throws Exception {
        String clientIp = "172.16.0.80";

        // Send 10 OPTIONS requests to a rate-limited route
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(options("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            }))
                    .andExpect(status().is(not(429)));
        }

        // Real POST should still have its full quota of 5 tokens
        LoginRequest loginPayload = LoginRequest.builder()
                .identifier("test.ratelimit@bazaarhub.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().is(not(429)))
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }

    @Test
    @DisplayName("Spoofed X-Forwarded-For headers from untrusted clients are ignored")
    void testSpoofedForwardedForHeaderIgnored() throws Exception {
        String untrustedRemoteIp = "198.51.100.99"; // Untrusted client

        // Attacker attempts to rotate X-Forwarded-For headers on each request
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(untrustedRemoteIp);
                                return req;
                            })
                            .header("X-Forwarded-For", "fake.ip." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                    .andExpect(status().is(not(429)));
        }

        // 6th request with a new fake IP MUST still be rate-limited because remoteAddr is tracked
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(untrustedRemoteIp);
                            return req;
                        })
                        .header("X-Forwarded-For", "new.fake.ip.100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("Different IP addresses receive independent rate limit buckets")
    void testIndependentIpQuotas() throws Exception {
        String clientA = "172.16.1.10";
        String clientB = "172.16.1.20";

        // Exhaust client A
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(clientA);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                    .andExpect(status().is(not(429)));
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(clientA);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                .andExpect(status().is(429));

        // Client B is unaffected and can perform requests
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(clientB);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"b@b.com\",\"password\":\"x\"}"))
                .andExpect(status().is(not(429)))
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }

    @Test
    @DisplayName("Unrelated endpoints (e.g. GET /api/v1/products) are not constrained by rate limiter rules")
    void testUnrelatedEndpointsNotRateLimited() throws Exception {
        String client = "172.16.2.1";

        // Query product catalog 10 times
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/products")
                            .with(req -> {
                                req.setRemoteAddr(client);
                                return req;
                            }))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Filter returns HTTP 503 when infrastructure failure occurs in FAIL_CLOSED mode")
    void testInfrastructureFailureReturns503() throws Exception {
        // Create an instance of filter with mock service returning infrastructureError
        RateLimiterService failingService = new RateLimiterService() {
            @Override
            public RateLimitResult tryAcquire(String clientKey, RateLimitRule rule) {
                return RateLimitResult.infrastructureError();
            }

            @Override
            public void reset(String clientKey, RateLimitRule rule) {}

            @Override
            public String getProviderType() {
                return "MOCK_FAILING_REDIS";
            }
        };

        ClientIpResolver ipResolver = new ClientIpResolver(new RateLimitConfigProperties());
        RateLimitingFilter filter = new RateLimitingFilter(
                failingService,
                ipResolver,
                new RateLimitConfigProperties(),
                objectMapper
        );

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.1");

        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain filterChain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, filterChain);

        org.junit.jupiter.api.Assertions.assertEquals(503, response.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("Service Temporarily Unavailable"));
    }
}
