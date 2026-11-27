package com.bazaarhub.backend.payment.infrastructure.gateway;

import com.bazaarhub.backend.payment.domain.exception.PaymentGatewayException;
import com.bazaarhub.backend.payment.infrastructure.config.RazorpayProperties;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayOrderResponse;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayPaymentDetails;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayRefundResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

@Component
public class RazorpayPaymentGatewayImpl implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGatewayImpl.class);
    private static final String BASE_URL = "https://api.razorpay.com/v1";

    private final RazorpayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public RazorpayPaymentGatewayImpl(RazorpayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }

    public RazorpayPaymentGatewayImpl(RazorpayProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public RazorpayOrderResponse createRazorpayOrder(String receipt, BigDecimal amount, String currency, Map<String, String> notes) {
        validateCredentials();

        long amountInPaise = toPaise(amount);
        String finalCurrency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "INR";

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amountInPaise);
        payload.put("currency", finalCurrency);
        payload.put("receipt", receipt);
        if (notes != null && !notes.isEmpty()) {
            payload.put("notes", notes);
        }

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/orders"))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Authorization", getBasicAuthHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                String id = root.path("id").asText();
                long respAmount = root.path("amount").asLong();
                String respCurrency = root.path("currency").asText();
                String respStatus = root.path("status").asText();

                return RazorpayOrderResponse.builder()
                        .id(id)
                        .amount(respAmount)
                        .amountDecimal(fromPaise(respAmount))
                        .currency(respCurrency)
                        .receipt(receipt)
                        .status(respStatus)
                        .build();
            } else {
                log.error("[RAZORPAY ORDER FAILED] Status: {}, Body: {}", response.statusCode(), response.body());
                String errorDescription = parseErrorMessage(response.body(), "Failed to create Razorpay order");
                throw new PaymentGatewayException("Razorpay gateway error: " + errorDescription);
            }
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[RAZORPAY ORDER EXCEPTION] Error calling Razorpay API: {}", ex.getMessage());
            throw new PaymentGatewayException("Could not initiate transaction with Razorpay: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) {
            return false;
        }
        String secret = properties.getKeySecret();
        if (secret == null || secret.isBlank()) {
            log.warn("[RAZORPAY VERIFY] Cannot verify signature: Razorpay key secret is not configured");
            return false;
        }

        String payload = orderId + "|" + paymentId;
        return computeHmacSha256(payload, secret, signature);
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (rawBody == null || signature == null) {
            return false;
        }
        String webhookSecret = properties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("[RAZORPAY WEBHOOK] Cannot verify webhook signature: Webhook secret is not configured");
            return false;
        }

        return computeHmacSha256(rawBody, webhookSecret, signature);
    }

    @Override
    public RazorpayRefundResponse initiateRefund(String gatewayPaymentId, BigDecimal amount, String reason, String receipt) {
        validateCredentials();

        long amountInPaise = toPaise(amount);

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amountInPaise);
        if (reason != null && !reason.isBlank()) {
            payload.put("notes", Map.of("reason", reason));
        }
        if (receipt != null && !receipt.isBlank()) {
            payload.put("receipt", receipt);
        }

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/payments/" + gatewayPaymentId + "/refund"))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Authorization", getBasicAuthHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                String id = root.path("id").asText();
                String paymentId = root.path("payment_id").asText();
                long respAmount = root.path("amount").asLong();
                String respCurrency = root.path("currency").asText();
                String respStatus = root.path("status").asText();

                return RazorpayRefundResponse.builder()
                        .id(id)
                        .paymentId(paymentId)
                        .amount(respAmount)
                        .amountDecimal(fromPaise(respAmount))
                        .currency(respCurrency)
                        .status(respStatus)
                        .receipt(receipt)
                        .build();
            } else {
                log.error("[RAZORPAY REFUND FAILED] Status: {}, Body: {}", response.statusCode(), response.body());
                String errorDescription = parseErrorMessage(response.body(), "Failed to execute Razorpay refund");
                throw new PaymentGatewayException("Razorpay refund error: " + errorDescription);
            }
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[RAZORPAY REFUND EXCEPTION] Error calling Razorpay API: {}", ex.getMessage());
            throw new PaymentGatewayException("Could not execute refund with Razorpay: " + ex.getMessage(), ex);
        }
    }

    @Override
    public RazorpayPaymentDetails fetchPaymentDetails(String gatewayPaymentId) {
        validateCredentials();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/payments/" + gatewayPaymentId))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Authorization", getBasicAuthHeader())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                long respAmount = root.path("amount").asLong();
                return RazorpayPaymentDetails.builder()
                        .id(root.path("id").asText())
                        .orderId(root.path("order_id").asText(null))
                        .amount(respAmount)
                        .amountDecimal(fromPaise(respAmount))
                        .currency(root.path("currency").asText("INR"))
                        .status(root.path("status").asText())
                        .method(root.path("method").asText(null))
                        .captured(root.path("captured").asBoolean(false))
                        .email(root.path("email").asText(null))
                        .contact(root.path("contact").asText(null))
                        .build();
            } else {
                String errorDescription = parseErrorMessage(response.body(), "Failed to fetch Razorpay payment details");
                throw new PaymentGatewayException("Razorpay fetch error: " + errorDescription);
            }
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentGatewayException("Could not fetch payment status from Razorpay: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getPublicKeyId() {
        return properties.getKeyId();
    }

    private void validateCredentials() {
        if (properties.getKeyId() == null || properties.getKeyId().isBlank() ||
                properties.getKeySecret() == null || properties.getKeySecret().isBlank()) {
            throw new PaymentGatewayException("Razorpay gateway credentials are not configured. Please verify environment variables.");
        }
    }

    private String getBasicAuthHeader() {
        String auth = properties.getKeyId() + ":" + properties.getKeySecret();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    private boolean computeHmacSha256(String data, String secret, String expectedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.error("[RAZORPAY HMAC ERROR] Signature computation failed: {}", ex.getMessage());
            return false;
        }
    }

    private long toPaise(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private BigDecimal fromPaise(long paise) {
        return BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String parseErrorMessage(String responseBody, String defaultMessage) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String description = error.path("description").asText(null);
                if (description != null) return description;
            }
        } catch (Exception ignored) {
        }
        return defaultMessage;
    }
}
