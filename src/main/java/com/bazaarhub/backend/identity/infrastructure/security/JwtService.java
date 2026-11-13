package com.bazaarhub.backend.identity.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Enterprise JWT service providing secure token generation, HMAC-SHA256 signature verification,
 * expiration validation, and claim extraction.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret:BazaarHubEnterpriseSuperSecureSecretKeyForJwtSigningMustBeAtLeast256Bits2026!}")
    private String secretKeyString;

    @Value("${jwt.access-token-expiration:900000}") // 15 minutes default in ms (secure short-lived access token)
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7 days default in ms
    private long refreshTokenExpiration;

    @Value("${jwt.issuer:bazaarhub-backend}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes) in length.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("[JWT SERVICE] Initialized with secure HMAC-SHA signing key. Issuer: '{}', Access Token TTL: {} ms.",
                issuer, accessTokenExpiration);
    }

    public String generateAccessToken(Long userId, String email, String phone, String fullName,
                                      Collection<String> roles, Collection<String> permissions) {
        return generateAccessToken(userId, email, phone, fullName, null, roles, permissions);
    }

    public String generateAccessToken(Long userId, String email, String phone, String fullName, String deviceId,
                                      Collection<String> roles, Collection<String> permissions) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("email", email);
        extraClaims.put("phone", phone);
        extraClaims.put("fullName", fullName);
        if (deviceId != null && !deviceId.isBlank()) {
            extraClaims.put("deviceId", deviceId.trim());
        }
        extraClaims.put("roles", roles != null ? new ArrayList<>(roles) : Collections.emptyList());
        extraClaims.put("permissions", permissions != null ? new ArrayList<>(permissions) : Collections.emptyList());

        return buildToken(extraClaims, String.valueOf(userId), accessTokenExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return subject != null ? Long.parseLong(subject) : null;
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    public String extractDeviceId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("deviceId", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        List<?> roles = claims.get("roles", List.class);
        if (roles == null) return Collections.emptyList();
        return (List<String>) roles;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Claims claims = extractAllClaims(token);
        List<?> permissions = claims.get("permissions", List.class);
        if (permissions == null) return Collections.emptyList();
        return (List<String>) permissions;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("[JWT SERVICE] Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
