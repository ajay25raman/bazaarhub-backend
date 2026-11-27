package com.bazaarhub.backend.payment;

import com.bazaarhub.backend.payment.domain.exception.PaymentGatewayException;
import com.bazaarhub.backend.payment.infrastructure.config.RazorpayProperties;
import com.bazaarhub.backend.payment.infrastructure.gateway.RazorpayPaymentGatewayImpl;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayOrderResponse;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayPaymentDetails;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayRefundResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentGatewayUnitTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private RazorpayProperties properties;
    private ObjectMapper objectMapper;
    private RazorpayPaymentGatewayImpl gateway;

    private static final String TEST_KEY_ID = "rzp_test_sample123";
    private static final String TEST_KEY_SECRET = "secret_key_456";
    private static final String TEST_WEBHOOK_SECRET = "webhook_secret_789";

    @BeforeEach
    void setUp() {
        properties = new RazorpayProperties();
        properties.getKey().setId(TEST_KEY_ID);
        properties.getKey().setSecret(TEST_KEY_SECRET);
        properties.getWebhook().setSecret(TEST_WEBHOOK_SECRET);
        properties.setConnectTimeoutMs(5000);
        properties.setReadTimeoutMs(10000);

        objectMapper = new ObjectMapper();
        gateway = new RazorpayPaymentGatewayImpl(properties, objectMapper, httpClient);
    }

    @Test
    @DisplayName("Verify valid payment signature calculation with HMAC SHA256")
    void testVerifyPaymentSignature_Success() throws Exception {
        String orderId = "order_O8xG8x123abcde";
        String paymentId = "pay_P9yH9y456fghij";
        String data = orderId + "|" + paymentId;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_KEY_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expectedSignature = HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));

        boolean result = gateway.verifyPaymentSignature(orderId, paymentId, expectedSignature);
        assertTrue(result, "Signature must match expected HMAC SHA256 hex");
    }

    @Test
    @DisplayName("Verify tampered payment signature fails verification")
    void testVerifyPaymentSignature_Tampered_Fails() {
        String orderId = "order_O8xG8x123abcde";
        String paymentId = "pay_P9yH9y456fghij";
        String forgedSignature = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        boolean result = gateway.verifyPaymentSignature(orderId, paymentId, forgedSignature);
        assertFalse(result, "Forged signature must fail verification");
    }

    @Test
    @DisplayName("Verify payment signature returns false when arguments are null")
    void testVerifyPaymentSignature_NullInputs() {
        assertFalse(gateway.verifyPaymentSignature(null, "pay_123", "sig_123"));
        assertFalse(gateway.verifyPaymentSignature("order_123", null, "sig_123"));
        assertFalse(gateway.verifyPaymentSignature("order_123", "pay_123", null));
    }

    @Test
    @DisplayName("Verify valid webhook signature verification against raw payload")
    void testVerifyWebhookSignature_Success() throws Exception {
        String rawBody = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_123\"}}}}";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expectedSignature = HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));

        boolean result = gateway.verifyWebhookSignature(rawBody, expectedSignature);
        assertTrue(result, "Webhook signature must be verified against exact raw payload");
    }

    @Test
    @DisplayName("Verify tampered webhook payload fails signature verification")
    void testVerifyWebhookSignature_TamperedPayload_Fails() throws Exception {
        String originalBody = "{\"event\":\"payment.captured\"}";
        String tamperedBody = "{\"event\":\"payment.captured\",\"amount\":999999}";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String originalSignature = HexFormat.of().formatHex(mac.doFinal(originalBody.getBytes(StandardCharsets.UTF_8)));

        boolean result = gateway.verifyWebhookSignature(tamperedBody, originalSignature);
        assertFalse(result, "Tampered payload must fail signature check");
    }

    @Test
    @DisplayName("Create Razorpay Order invokes HTTP endpoint and parses response")
    void testCreateRazorpayOrder_Success() throws Exception {
        String jsonResponse = """
                {
                  "id": "order_EKwxwAgIt42hQC",
                  "entity": "order",
                  "amount": 149900,
                  "amount_paid": 0,
                  "amount_due": 149900,
                  "currency": "INR",
                  "receipt": "ORD-20261114-001",
                  "status": "created"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        RazorpayOrderResponse response = gateway.createRazorpayOrder(
                "ORD-20261114-001", new BigDecimal("1499.00"), "INR", Map.of("orderId", "101"));

        assertNotNull(response);
        assertEquals("order_EKwxwAgIt42hQC", response.getId());
        assertEquals(149900L, response.getAmount());
        assertEquals(new BigDecimal("1499.00"), response.getAmountDecimal());
        assertEquals("INR", response.getCurrency());
        assertEquals("created", response.getStatus());
    }

    @Test
    @DisplayName("Create Razorpay Order throws PaymentGatewayException on gateway error status")
    void testCreateRazorpayOrder_GatewayError() throws Exception {
        String errorJson = """
                {
                  "error": {
                    "code": "BAD_REQUEST_ERROR",
                    "description": "Order amount is less than minimum permissible amount"
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn(errorJson);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                gateway.createRazorpayOrder("ORD-001", new BigDecimal("0.50"), "INR", null));

        assertTrue(ex.getMessage().contains("Order amount is less than minimum permissible amount"));
    }

    @Test
    @DisplayName("Initiate refund invokes Razorpay API and returns refund response")
    void testInitiateRefund_Success() throws Exception {
        String jsonResponse = """
                {
                  "id": "rfnd_FP8j7n28xQC123",
                  "entity": "refund",
                  "amount": 50000,
                  "currency": "INR",
                  "payment_id": "pay_P9yH9y456fghij",
                  "status": "processed"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        RazorpayRefundResponse refund = gateway.initiateRefund(
                "pay_P9yH9y456fghij", new BigDecimal("500.00"), "Defective unit", "RFND-001");

        assertNotNull(refund);
        assertEquals("rfnd_FP8j7n28xQC123", refund.getId());
        assertEquals("pay_P9yH9y456fghij", refund.getPaymentId());
        assertEquals(50000L, refund.getAmount());
        assertEquals(new BigDecimal("500.00"), refund.getAmountDecimal());
        assertEquals("processed", refund.getStatus());
    }

    @Test
    @DisplayName("Fetch payment details retrieves payment information")
    void testFetchPaymentDetails_Success() throws Exception {
        String jsonResponse = """
                {
                  "id": "pay_P9yH9y456fghij",
                  "entity": "payment",
                  "amount": 149900,
                  "currency": "INR",
                  "status": "captured",
                  "order_id": "order_EKwxwAgIt42hQC",
                  "method": "upi",
                  "email": "customer@example.com",
                  "contact": "+919876543210"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        RazorpayPaymentDetails details = gateway.fetchPaymentDetails("pay_P9yH9y456fghij");

        assertNotNull(details);
        assertEquals("pay_P9yH9y456fghij", details.getId());
        assertEquals("upi", details.getMethod());
        assertEquals("captured", details.getStatus());
        assertEquals("customer@example.com", details.getEmail());
    }
}
