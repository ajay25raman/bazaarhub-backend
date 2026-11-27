package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverUnitTest {

    private RateLimitConfigProperties properties;
    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new RateLimitConfigProperties();
        properties.setTrustedProxies("127.0.0.1,::1,10.0.0.1");
        resolver = new ClientIpResolver(properties);
    }

    @Test
    @DisplayName("Direct remote address is returned when request is not behind a trusted proxy")
    void testDirectUntrustedRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.195");
        request.addHeader("X-Forwarded-For", "198.51.100.22"); // Spoofed header attempt

        String resolvedIp = resolver.resolveClientIp(request);

        assertEquals("203.0.113.195", resolvedIp, "Must ignore X-Forwarded-For when remoteAddr is untrusted");
    }

    @Test
    @DisplayName("X-Forwarded-For client IP is resolved when immediate peer is a trusted proxy")
    void testTrustedProxyForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // Trusted proxy
        request.addHeader("X-Forwarded-For", "198.51.100.45");

        String resolvedIp = resolver.resolveClientIp(request);

        assertEquals("198.51.100.45", resolvedIp);
    }

    @Test
    @DisplayName("Multi-hop proxy chain correctly identifies the client IP before trusted proxies")
    void testMultiHopProxyChain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); // Trusted localhost proxy
        // Format: client, intermediary-proxy, ingress-proxy
        request.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");

        String resolvedIp = resolver.resolveClientIp(request);

        assertEquals("203.0.113.50", resolvedIp);
    }

    @Test
    @DisplayName("Malformed forwarded IP tokens are rejected and fallback to remoteAddr")
    void testMalformedForwardedTokensIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "invalid<script>, not-an-ip, 999.999.999.999");

        String resolvedIp = resolver.resolveClientIp(request);

        assertEquals("127.0.0.1", resolvedIp, "Malformed tokens must not be accepted");
    }

    @Test
    @DisplayName("IPv6 addresses are validated and supported")
    void testIpv6AddressSupported() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        String resolvedIp = resolver.resolveClientIp(request);

        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", resolvedIp);
    }

    @Test
    @DisplayName("IP validation recognizes valid and invalid formats accurately")
    void testIpValidation() {
        assertTrue(resolver.isValidIp("127.0.0.1"));
        assertTrue(resolver.isValidIp("192.168.1.254"));
        assertTrue(resolver.isValidIp("10.0.0.1"));
        assertTrue(resolver.isValidIp("::1"));
        assertTrue(resolver.isValidIp("2001:db8::1"));

        assertFalse(resolver.isValidIp(null));
        assertFalse(resolver.isValidIp(""));
        assertFalse(resolver.isValidIp("256.1.1.1"));
        assertFalse(resolver.isValidIp("1.2.3"));
        assertFalse(resolver.isValidIp("localhost"));
        assertFalse(resolver.isValidIp("drop table users;"));
    }
}
