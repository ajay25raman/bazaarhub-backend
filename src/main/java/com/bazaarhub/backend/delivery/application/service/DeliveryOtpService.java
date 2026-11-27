package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing secure delivery completion OTPs.
 * Adheres strictly to enterprise cryptographic standards:
 * - SecureRandom generation of 6-digit OTP
 * - Salted SHA-256 hash storage (never stored in plaintext)
 * - Strict brute-force protection (max 3 failed attempts)
 * - Configurable expiration window (default 2 hours)
 * - Zero plaintext leakage in logs or API payloads
 */
@Service
public class DeliveryOtpService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryOtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${bazaarhub.delivery.otp-expiration-minutes:120}")
    private int expirationMinutes;

    @Value("${bazaarhub.delivery.otp-max-retries:3}")
    private int maxRetries;

    public record OtpMetadata(String saltedHash, String salt, Instant expiresAt, int attemptCount) {}

    // In-memory thread-safe store for active delivery OTP metadata mapped by shipment ID
    private final Map<Long, OtpMetadata> otpStore = new ConcurrentHashMap<>();

    /**
     * Generates a new cryptographically secure 6-digit OTP for the given shipment.
     * Stores its salted hash and returns the plaintext OTP strictly for transmission to the customer.
     *
     * @param shipmentId ID of the shipment entering OUT_FOR_DELIVERY
     * @return 6-digit plaintext OTP string (for customer dispatch only)
     */
    public String generateAndStoreOtp(Long shipmentId) {
        int otpCode = 100000 + SECURE_RANDOM.nextInt(900000);
        String plainOtp = String.valueOf(otpCode);

        byte[] saltBytes = new byte[16];
        SECURE_RANDOM.nextBytes(saltBytes);
        String salt = HexFormat.of().formatHex(saltBytes);

        String saltedHash = hashWithSalt(plainOtp, salt);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expirationMinutes));

        otpStore.put(shipmentId, new OtpMetadata(saltedHash, salt, expiresAt, 0));
        log.info("[OTP GENERATED] Secure delivery OTP generated for shipment ID: {}. Expires at: {}",
                shipmentId, expiresAt);

        return plainOtp;
    }

    /**
     * Verifies a delivery OTP submitted by the delivery boy.
     *
     * @param shipmentId Target shipment ID
     * @param plainOtp Plaintext OTP entered by customer
     * @throws InvalidDeliveryOtpException if invalid, expired, or retry limit exceeded
     */
    public void verifyOtp(Long shipmentId, String plainOtp) {
        OtpMetadata metadata = otpStore.get(shipmentId);
        if (metadata == null) {
            log.warn("[OTP VERIFY FAILED] No active OTP record found for shipment ID: {}", shipmentId);
            throw new InvalidDeliveryOtpException("No active delivery OTP found for this shipment. Please request a new OTP.");
        }

        if (Instant.now().isAfter(metadata.expiresAt())) {
            otpStore.remove(shipmentId);
            log.warn("[OTP EXPIRED] Delivery OTP expired for shipment ID: {}", shipmentId);
            throw new InvalidDeliveryOtpException("Delivery OTP has expired. Please re-trigger out-for-delivery notification.");
        }

        if (metadata.attemptCount() >= maxRetries) {
            otpStore.remove(shipmentId);
            log.warn("[OTP LOCKED] Delivery OTP locked due to exceeding max attempts for shipment ID: {}", shipmentId);
            throw new InvalidDeliveryOtpException("Maximum verification attempts exceeded. Delivery OTP has been locked for security.");
        }

        if (plainOtp == null || plainOtp.trim().length() != 6) {
            recordFailedAttempt(shipmentId, metadata);
            throw new InvalidDeliveryOtpException("Invalid OTP format. Delivery OTP must be exactly 6 digits.");
        }

        String computedHash = hashWithSalt(plainOtp.trim(), metadata.salt());
        if (!MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8),
                                  metadata.saltedHash().getBytes(StandardCharsets.UTF_8))) {
            recordFailedAttempt(shipmentId, metadata);
            int remaining = maxRetries - (metadata.attemptCount() + 1);
            log.warn("[OTP MISMATCH] Invalid OTP entered for shipment ID: {}. Remaining attempts: {}", shipmentId, remaining);
            throw new InvalidDeliveryOtpException("Incorrect delivery OTP. Remaining attempts: " + remaining);
        }

        // Successfully verified: invalidate OTP immediately to prevent reuse
        otpStore.remove(shipmentId);
        log.info("[OTP VERIFIED] Delivery OTP successfully verified and invalidated for shipment ID: {}", shipmentId);
    }

    /**
     * Explicitly invalidates an active OTP upon cancellation or manual reset.
     */
    public void invalidateOtp(Long shipmentId) {
        otpStore.remove(shipmentId);
        log.info("[OTP INVALIDATED] OTP invalidated for shipment ID: {}", shipmentId);
    }

    private void recordFailedAttempt(Long shipmentId, OtpMetadata metadata) {
        otpStore.put(shipmentId, new OtpMetadata(
                metadata.saltedHash(),
                metadata.salt(),
                metadata.expiresAt(),
                metadata.attemptCount() + 1));
    }

    private String hashWithSalt(String plainText, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest algorithm not available", e);
        }
    }
}
