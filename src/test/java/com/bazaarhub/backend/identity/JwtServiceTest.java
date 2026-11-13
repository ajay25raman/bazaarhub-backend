package com.bazaarhub.backend.identity;

import com.bazaarhub.backend.identity.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "SuperSecretKeyForTestingJwtServiceMustBeAtLeast256BitsLong2026!";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyString", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "bazaarhub-backend");
        jwtService.init();
    }

    @Test
    @DisplayName("Generate access token and extract correct claims")
    void testGenerateAndExtractClaims() {
        Long userId = 42L;
        String email = "testuser@bazaarhub.com";
        String phone = "+919876543210";
        String fullName = "Test User";
        List<String> roles = List.of("ROLE_CUSTOMER");
        List<String> permissions = List.of("PRODUCT_READ", "CART_WRITE");

        String token = jwtService.generateAccessToken(userId, email, phone, fullName, roles, permissions);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token));

        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(roles, jwtService.extractRoles(token));
        assertEquals(permissions, jwtService.extractPermissions(token));
    }

    @Test
    @DisplayName("Generate access token with device ID claim and extract it")
    void testGenerateWithDeviceId() {
        String token = jwtService.generateAccessToken(10L, "device@bazaarhub.com", "+919999999999", "Dev User", "device-xyz-123", List.of(), List.of());
        assertNotNull(token);
        assertEquals("device-xyz-123", jwtService.extractDeviceId(token));
    }

    @Test
    @DisplayName("Invalid signature token must be rejected")
    void testInvalidSignatureRejected() {
        Long userId = 99L;
        String token = jwtService.generateAccessToken(userId, "tampered@bazaarhub.com", "+919000000000", "Tamper", List.of("ROLE_ADMIN"), List.of());

        // Tamper with the token payload
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        String tamperedToken = parts[0] + "." + parts[1] + "extra." + parts[2];

        assertFalse(jwtService.isTokenValid(tamperedToken));
    }

    @Test
    @DisplayName("Expired token must fail validation")
    void testExpiredTokenValidation() {
        JwtService shortLivedJwtService = new JwtService();
        ReflectionTestUtils.setField(shortLivedJwtService, "secretKeyString", SECRET);
        ReflectionTestUtils.setField(shortLivedJwtService, "accessTokenExpiration", -1000L); // Already expired
        ReflectionTestUtils.setField(shortLivedJwtService, "refreshTokenExpiration", -1000L);
        ReflectionTestUtils.setField(shortLivedJwtService, "issuer", "bazaarhub-backend");
        shortLivedJwtService.init();

        String expiredToken = shortLivedJwtService.generateAccessToken(1L, "exp@bazaarhub.com", null, "Expired", List.of(), List.of());

        assertFalse(shortLivedJwtService.isTokenValid(expiredToken));
    }

    @Test
    @DisplayName("Malformed token must return false on validation")
    void testMalformedTokenValidation() {
        assertFalse(jwtService.isTokenValid("not.a.valid.jwt.token"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid(null));
    }
}
