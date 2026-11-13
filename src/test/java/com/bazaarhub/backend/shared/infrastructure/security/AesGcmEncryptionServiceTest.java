package com.bazaarhub.backend.shared.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmEncryptionServiceTest {

    private AesGcmEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new AesGcmEncryptionService("TestEncryptionSecretKeyForAes256Gcm32B!");
    }

    @Test
    @DisplayName("Encrypt and decrypt round-trip restores exact plaintext")
    void testEncryptDecryptRoundTrip() {
        String plainAccount = "50100456789012";
        String encrypted = encryptionService.encrypt(plainAccount);

        assertNotNull(encrypted);
        assertNotEquals(plainAccount, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plainAccount, decrypted);
    }

    @Test
    @DisplayName("Two encryptions of the same plaintext produce different ciphertexts due to randomized IV")
    void testRandomizedIv() {
        String plain = "HDFC0001234";
        String cipher1 = encryptionService.encrypt(plain);
        String cipher2 = encryptionService.encrypt(plain);

        assertNotNull(cipher1);
        assertNotNull(cipher2);
        assertNotEquals(cipher1, cipher2, "Ciphertexts must differ due to random IV/nonce generation");

        assertEquals(plain, encryptionService.decrypt(cipher1));
        assertEquals(plain, encryptionService.decrypt(cipher2));
    }

    @Test
    @DisplayName("Tampered ciphertext fails authentication and throws exception")
    void testTamperedCiphertextFails() {
        String encrypted = encryptionService.encrypt("SensitiveBankData123");
        byte[] bytes = Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 0x01; // Tamper with the GCM authentication tag
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThrows(IllegalStateException.class, () -> encryptionService.decrypt(tampered));
    }

    @Test
    @DisplayName("Masking bank account numbers shows only last 4 digits")
    void testMaskAccountNumber() {
        assertEquals("XXXXXXXX9012", encryptionService.maskAccountNumber("501004569012"));
        assertEquals("XXXX1234", encryptionService.maskAccountNumber("1234"));
        assertEquals("XXXX", encryptionService.maskAccountNumber(""));
        assertEquals("XXXX", encryptionService.maskAccountNumber(null));
    }

    @Test
    @DisplayName("Masking IFSC codes preserves bank prefix and branch suffix")
    void testMaskIfscCode() {
        assertEquals("HDFC****234", encryptionService.maskIfscCode("HDFC0001234"));
        assertEquals("SBIN****001", encryptionService.maskIfscCode("SBIN0000001"));
        assertEquals("XXXX", encryptionService.maskIfscCode(null));
    }
}
