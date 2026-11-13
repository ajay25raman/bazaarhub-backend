package com.bazaarhub.backend.shared.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise AES-256-GCM authenticated encryption service for sensitive data (bank accounts, IFSC codes).
 *
 * <p>Security features:
 * <ul>
 *   <li>Algorithm: AES/GCM/NoPadding (Authenticated Encryption with Associated Data).</li>
 *   <li>128-bit authentication tag guarantees confidentiality and ciphertext integrity.</li>
 *   <li>Cryptographically secure 96-bit (12-byte) IV generated per encryption.</li>
 *   <li>Zero plaintext logging.</li>
 *   <li>Sensitive field masking helpers for safe REST responses.</li>
 * </ul>
 */
@Service
public class AesGcmEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesGcmEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptionService(
            @Value("${bazaarhub.security.encryption.key:BazaarHubEnterpriseEncryptionKey32B!}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] validKey = new byte[32]; // 256 bits
        System.arraycopy(keyBytes, 0, validKey, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(validKey, "AES");
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Output format: Base64(IV [12 bytes] + Ciphertext + Tag [16 bytes]).
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception ex) {
            log.error("[ENCRYPTION ERROR] Failed to encrypt sensitive data payload: {}", ex.getMessage());
            throw new IllegalStateException("Failed to encrypt sensitive data securely", ex);
        }
    }

    /**
     * Decrypts Base64(IV + Ciphertext + Tag) back to plaintext string.
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            if (decoded.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted payload length");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("[DECRYPTION ERROR] Failed to decrypt sensitive data payload: {}", ex.getMessage());
            throw new IllegalStateException("Failed to decrypt sensitive data securely", ex);
        }
    }

    /**
     * Masks an account number, showing only the last 4 digits (e.g., "XXXXXXXX1234").
     */
    public String maskAccountNumber(String plainAccountNumber) {
        if (plainAccountNumber == null || plainAccountNumber.isBlank()) {
            return "XXXX";
        }
        String trimmed = plainAccountNumber.trim();
        if (trimmed.length() <= 4) {
            return "XXXX" + trimmed;
        }
        String lastFour = trimmed.substring(trimmed.length() - 4);
        return "X".repeat(trimmed.length() - 4) + lastFour;
    }

    /**
     * Masks an IFSC code (e.g., "HDFC0001234" -> "HDFC***1234").
     */
    public String maskIfscCode(String plainIfscCode) {
        if (plainIfscCode == null || plainIfscCode.isBlank()) {
            return "XXXX";
        }
        String trimmed = plainIfscCode.trim();
        if (trimmed.length() <= 6) {
            return trimmed.substring(0, Math.min(2, trimmed.length())) + "***";
        }
        String prefix = trimmed.substring(0, 4);
        String suffix = trimmed.substring(trimmed.length() - 3);
        return prefix + "*".repeat(trimmed.length() - 7) + suffix;
    }
}
