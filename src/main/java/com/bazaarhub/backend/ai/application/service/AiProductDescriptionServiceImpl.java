package com.bazaarhub.backend.ai.application.service;

import com.bazaarhub.backend.ai.application.dto.request.AiGenerateDescriptionRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiProductDescriptionResponse;
import com.bazaarhub.backend.ai.infrastructure.client.AiChatMessage;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.ai.infrastructure.security.AiPromptSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiProductDescriptionServiceImpl implements AiProductDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(AiProductDescriptionServiceImpl.class);

    private final AiClient aiClient;
    private final AiPromptSanitizer promptSanitizer;
    private final ObjectMapper objectMapper;

    public AiProductDescriptionServiceImpl(AiClient aiClient,
                                           AiPromptSanitizer promptSanitizer,
                                           ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.promptSanitizer = promptSanitizer;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProductDescriptionResponse generateContent(AiGenerateDescriptionRequest request, Long requestingUserId) {
        String cleanName = promptSanitizer.sanitize(request.getProductName());
        String cleanCategory = request.getCategoryName() != null ? promptSanitizer.sanitize(request.getCategoryName()) : "General";
        String cleanBrand = request.getBrandName() != null ? promptSanitizer.sanitize(request.getBrandName()) : "";
        String cleanAudience = request.getTargetAudience() != null ? promptSanitizer.sanitize(request.getTargetAudience()) : "Shoppers";

        List<String> features = request.getKeyFeatures() != null && !request.getKeyFeatures().isEmpty()
                ? request.getKeyFeatures().stream().map(promptSanitizer::sanitize).filter(s -> !s.isBlank()).toList()
                : List.of("High durability and premium craftsmanship", "Designed for daily convenience and reliable performance");

        try {
            String systemPrompt = """
                You are a senior e-commerce copywriter and SEO specialist for BazaarHub marketplace.
                Generate a high-converting, professional product listing draft in valid JSON format.
                JSON structure:
                {
                  "suggestedTitles": ["title1", "title2", "title3"],
                  "shortDescription": "100-word concise pitch for mobile listings",
                  "longDescription": "Detailed markdown formatted description with specifications",
                  "keyFeatures": ["feature 1", "feature 2", "feature 3", "feature 4"],
                  "seoTitle": "Max 60 character title including brand and primary keyword",
                  "seoDescription": "Max 160 character meta description with CTA"
                }
                Output ONLY the raw JSON object, without backticks or markdown fences.
                """;

            String userPrompt = String.format("""
                Product Name: %s
                Category: %s
                Brand: %s
                Target Audience: %s
                Key Features / Specs: %s
                """, cleanName, cleanCategory, cleanBrand, cleanAudience, String.join(", ", features));

            List<AiChatMessage> messages = List.of(
                    AiChatMessage.system(systemPrompt),
                    AiChatMessage.user(userPrompt)
            );

            AiCompletionResponse completion = aiClient.completeChat(messages, 800, 0.3);
            String rawJson = completion.getContent().trim();
            if (rawJson.startsWith("```json")) {
                rawJson = rawJson.substring(7);
            }
            if (rawJson.startsWith("```")) {
                rawJson = rawJson.substring(3);
            }
            if (rawJson.endsWith("```")) {
                rawJson = rawJson.substring(0, rawJson.length() - 3);
            }
            rawJson = rawJson.trim();

            JsonNode node = objectMapper.readTree(rawJson);
            List<String> parsedTitles = new ArrayList<>();
            if (node.has("suggestedTitles") && node.get("suggestedTitles").isArray()) {
                node.get("suggestedTitles").forEach(t -> parsedTitles.add(t.asText()));
            }
            List<String> titles = parsedTitles.isEmpty() ? generateFallbackTitles(cleanName, cleanBrand) : parsedTitles;

            List<String> parsedFeatures = new ArrayList<>();
            if (node.has("keyFeatures") && node.get("keyFeatures").isArray()) {
                node.get("keyFeatures").forEach(f -> parsedFeatures.add(f.asText()));
            }
            List<String> outputFeatures = parsedFeatures.isEmpty() ? features : parsedFeatures;

            String shortDesc = node.path("shortDescription").asText("");
            if (shortDesc.isBlank()) {
                shortDesc = generateFallbackShort(cleanName, cleanBrand, cleanCategory);
            }

            String longDesc = node.path("longDescription").asText("");
            if (longDesc.isBlank()) {
                longDesc = generateFallbackLong(cleanName, cleanBrand, features);
            }

            String seoTitle = node.path("seoTitle").asText("");
            if (seoTitle.isBlank() || seoTitle.length() > 60) {
                seoTitle = cleanName + " | BazaarHub";
                if (seoTitle.length() > 60) seoTitle = seoTitle.substring(0, 57) + "...";
            }

            String seoDesc = node.path("seoDescription").asText("");
            if (seoDesc.isBlank() || seoDesc.length() > 160) {
                seoDesc = "Buy " + cleanName + " online at the best price on BazaarHub. Fast delivery and secure payments.";
            }

            return AiProductDescriptionResponse.builder()
                    .suggestedTitles(titles)
                    .shortDescription(shortDesc)
                    .longDescription(longDesc)
                    .keyFeatures(outputFeatures)
                    .seoTitle(seoTitle)
                    .seoDescription(seoDesc)
                    .status("DRAFT")
                    .modelName(completion.getModel())
                    .build();

        } catch (Exception ex) {
            log.warn("[AI DESCRIPTION FALLBACK] AI generation failed ({}), returning deterministic draft template", ex.getMessage());
            return buildDeterministicFallback(cleanName, cleanBrand, cleanCategory, features);
        }
    }

    private AiProductDescriptionResponse buildDeterministicFallback(String name, String brand, String category, List<String> features) {
        List<String> titles = generateFallbackTitles(name, brand);
        String shortDesc = generateFallbackShort(name, brand, category);
        String longDesc = generateFallbackLong(name, brand, features);
        String seoTitle = (brand.isBlank() ? "" : brand + " ") + name + " | BazaarHub";
        if (seoTitle.length() > 60) seoTitle = seoTitle.substring(0, 57) + "...";
        String seoDesc = "Shop " + name + " online at BazaarHub. Experience premium quality, verified seller warranty, and fast delivery across India.";
        if (seoDesc.length() > 160) seoDesc = seoDesc.substring(0, 157) + "...";

        return AiProductDescriptionResponse.builder()
                .suggestedTitles(titles)
                .shortDescription(shortDesc)
                .longDescription(longDesc)
                .keyFeatures(features)
                .seoTitle(seoTitle)
                .seoDescription(seoDesc)
                .status("DRAFT")
                .modelName("bazaarhub-catalog-template-engine")
                .build();
    }

    private List<String> generateFallbackTitles(String name, String brand) {
        String b = brand.isBlank() ? "" : brand + " ";
        return List.of(
                b + name + " - Premium Quality & High Performance",
                "Official " + b + name + " for Everyday Use",
                b + name + " (Latest Edition) with Complete Warranty"
        );
    }

    private String generateFallbackShort(String name, String brand, String category) {
        String b = brand.isBlank() ? "The " : "The " + brand + " ";
        return b + name + " delivers exceptional quality and convenience in the " + category + " category. Designed for longevity, superior finish, and reliable everyday usage with verified merchant guarantee.";
    }

    private String generateFallbackLong(String name, String brand, List<String> features) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Overview\n");
        sb.append("Introducing the ").append(brand.isBlank() ? "" : brand + " ").append(name)
                .append(", precision-engineered to meet stringent marketplace standards. Built for discerning users who demand reliability and performance.\n\n");
        sb.append("## Key Highlights & Specifications\n");
        for (String f : features) {
            sb.append("- **").append(f).append("**\n");
        }
        sb.append("\n## Why Choose This Product?\n");
        sb.append("- Verified genuine seller inventory backed by BazaarHub buyer protection.\n");
        sb.append("- Standard 7-day replacement guarantee on manufacturing defects.\n");
        sb.append("- Secure doorstep delivery with end-to-end tracking.\n");
        return sb.toString();
    }
}
