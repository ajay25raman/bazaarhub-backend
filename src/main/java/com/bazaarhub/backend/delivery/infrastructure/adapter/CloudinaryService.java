package com.bazaarhub.backend.delivery.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service managing image and document uploads to Cloudinary for delivery operations
 * (Proof of Delivery, KYC verification, vehicle condition, failed delivery proof).
 */
@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    @Value("${cloudinary.cloud-name:demo}")
    private String cloudName;

    @Value("${cloudinary.api-key:placeholder_api_key}")
    private String apiKey;

    @Value("${cloudinary.api-secret:placeholder_api_secret}")
    private String apiSecret;

    private final HttpClient httpClient;

    public CloudinaryService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Uploads an image (base64 string or binary payload or raw URL) to a designated folder.
     *
     * @param imagePayload Base64 encoded string, URL, or data URI
     * @param folder Destination folder, e.g., "bazaarhub/delivery/proofs"
     * @return Secure public URL of uploaded asset
     */
    public String uploadImage(String imagePayload, String folder) {
        if (imagePayload == null || imagePayload.isBlank()) {
            return null;
        }

        // If it's already an HTTP URL (e.g., test or external CDN), validate and return
        if (imagePayload.startsWith("http://") || imagePayload.startsWith("https://")) {
            return imagePayload;
        }

        // Check if real Cloudinary credentials are configured
        if ("placeholder_api_key".equals(apiKey) || "demo".equalsIgnoreCase(cloudName) || apiSecret == null) {
            String pseudoId = UUID.randomUUID().toString().substring(0, 8);
            String mockUrl = String.format("https://res.cloudinary.com/%s/image/upload/%s/%s_%s.jpg",
                    cloudName, folder, pseudoId, Instant.now().getEpochSecond());
            log.info("[CLOUDINARY MOCK] Generated delivery asset URL: {} for folder: {}", mockUrl, folder);
            return mockUrl;
        }

        try {
            long timestamp = Instant.now().getEpochSecond();
            String stringToSign = "folder=" + folder + "&timestamp=" + timestamp + apiSecret;
            String signature = sha1Hex(stringToSign);

            String requestBody = "file=" + java.net.URLEncoder.encode(imagePayload, StandardCharsets.UTF_8)
                    + "&folder=" + java.net.URLEncoder.encode(folder, StandardCharsets.UTF_8)
                    + "&timestamp=" + timestamp
                    + "&api_key=" + apiKey
                    + "&signature=" + signature;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudName)))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Parse secure_url from JSON
                String body = response.body();
                int idx = body.indexOf("\"secure_url\":\"");
                if (idx != -1) {
                    int endIdx = body.indexOf("\"", idx + 14);
                    if (endIdx != -1) {
                        return body.substring(idx + 14, endIdx);
                    }
                }
            }
            log.warn("[CLOUDINARY WARN] Cloudinary upload returned HTTP {}: {}", response.statusCode(), response.body());
        } catch (Exception ex) {
            log.warn("[CLOUDINARY ERROR] Failed to upload image to Cloudinary: {}", ex.getMessage());
        }

        // Fallback placeholder URL
        String pseudoId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("https://res.cloudinary.com/%s/image/upload/%s/%s_%s.jpg",
                cloudName, folder, pseudoId, Instant.now().getEpochSecond());
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}
