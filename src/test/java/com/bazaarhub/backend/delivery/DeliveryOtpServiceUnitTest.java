package com.bazaarhub.backend.delivery;

import com.bazaarhub.backend.delivery.application.service.DeliveryOtpService;
import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOtpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryOtpServiceUnitTest {

    private DeliveryOtpService deliveryOtpService;

    @BeforeEach
    void setUp() {
        deliveryOtpService = new DeliveryOtpService();
        ReflectionTestUtils.setField(deliveryOtpService, "expirationMinutes", 120);
        ReflectionTestUtils.setField(deliveryOtpService, "maxRetries", 3);
    }

    @Test
    @DisplayName("Successfully generate 6-digit cryptographic OTP and verify with valid code")
    void testGenerateAndVerifyOtp_Success() {
        Long shipmentId = 101L;

        String plainOtp = deliveryOtpService.generateAndStoreOtp(shipmentId);

        assertNotNull(plainOtp);
        assertEquals(6, plainOtp.length());
        assertTrue(plainOtp.matches("^\\d{6}$"), "OTP must consist of 6 numeric digits");

        // Verify valid OTP
        assertDoesNotThrow(() -> deliveryOtpService.verifyOtp(shipmentId, plainOtp));

        // Once successfully verified, OTP must be immediately invalidated
        assertThrows(InvalidDeliveryOtpException.class,
                () -> deliveryOtpService.verifyOtp(shipmentId, plainOtp),
                "Used OTP must be invalidated immediately to prevent replay attacks");
    }

    @Test
    @DisplayName("Incorrect OTP entry decrements remaining attempts and throws InvalidDeliveryOtpException")
    void testVerifyOtp_IncorrectCode_DecrementsAttempts() {
        Long shipmentId = 102L;
        String plainOtp = deliveryOtpService.generateAndStoreOtp(shipmentId);

        String incorrectOtp = plainOtp.equals("123456") ? "654321" : "123456";

        InvalidDeliveryOtpException ex = assertThrows(InvalidDeliveryOtpException.class,
                () -> deliveryOtpService.verifyOtp(shipmentId, incorrectOtp));

        assertTrue(ex.getMessage().contains("Incorrect delivery OTP"), "Exception must state incorrect OTP");
        assertTrue(ex.getMessage().contains("Remaining attempts: 2"));
    }

    @Test
    @DisplayName("Exceeding maximum retry limit locks OTP and prevents further verification")
    void testVerifyOtp_ExceedsMaxRetries_LocksOtp() {
        Long shipmentId = 103L;
        String plainOtp = deliveryOtpService.generateAndStoreOtp(shipmentId);
        String wrongOtp = plainOtp.equals("111111") ? "222222" : "111111";

        // Attempt 1
        assertThrows(InvalidDeliveryOtpException.class, () -> deliveryOtpService.verifyOtp(shipmentId, wrongOtp));
        // Attempt 2
        assertThrows(InvalidDeliveryOtpException.class, () -> deliveryOtpService.verifyOtp(shipmentId, wrongOtp));
        // Attempt 3
        assertThrows(InvalidDeliveryOtpException.class, () -> deliveryOtpService.verifyOtp(shipmentId, wrongOtp));

        // Attempt 4 (Locked)
        InvalidDeliveryOtpException lockedEx = assertThrows(InvalidDeliveryOtpException.class,
                () -> deliveryOtpService.verifyOtp(shipmentId, wrongOtp));

        assertTrue(lockedEx.getMessage().contains("Maximum verification attempts exceeded") ||
                   lockedEx.getMessage().contains("locked"));
    }

    @Test
    @DisplayName("Verification fails if no active OTP exists for shipment")
    void testVerifyOtp_NoOtpActive_ThrowsException() {
        assertThrows(InvalidDeliveryOtpException.class,
                () -> deliveryOtpService.verifyOtp(999L, "123456"));
    }

    @Test
    @DisplayName("Manual OTP invalidation removes OTP from active store")
    void testInvalidateOtp_Success() {
        Long shipmentId = 104L;
        String plainOtp = deliveryOtpService.generateAndStoreOtp(shipmentId);

        deliveryOtpService.invalidateOtp(shipmentId);

        assertThrows(InvalidDeliveryOtpException.class,
                () -> deliveryOtpService.verifyOtp(shipmentId, plainOtp));
    }
}
