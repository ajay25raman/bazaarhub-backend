package com.bazaarhub.backend.ai.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI-generated rich product descriptions, specifications, and SEO metadata in draft status")
public class AiProductDescriptionResponse {

    @Schema(description = "3 High-converting product title alternatives", example = "[\"Bazaar Pro ANC Wireless Headphones - 50H Battery Life\", \"Bazaar Pro Bluetooth 5.4 Over-Ear Noise-Cancelling Headphones\", \"Bazaar Pro Wireless Studio Headphones with 40dB ANC\"]")
    private List<String> suggestedTitles;

    @Schema(description = "Concise summary description for product cards and mobile views (approx 100-150 words)", example = "Immerse yourself in acoustic clarity with the Bazaar Pro Wireless Headphones featuring advanced 40dB active noise cancellation...")
    private String shortDescription;

    @Schema(description = "Detailed long-form product description with structured features and specifications", example = "## Exceptional Sound Engineering\nEquipped with 40mm dynamic bio-cellulose drivers...")
    private String longDescription;

    @Schema(description = "Key bulleted features formatted for marketplace listings", example = "[\"Hybrid Active Noise Cancellation up to 40dB\", \"Ultra-long 50-hour playback with quick USB-C recharge\"]")
    private List<String> keyFeatures;

    @Schema(description = "SEO meta title optimized under 60 characters", example = "Bazaar Pro ANC Wireless Headphones - 50H Playtime | BazaarHub")
    private String seoTitle;

    @Schema(description = "SEO meta description optimized under 160 characters", example = "Experience 40dB Active Noise Cancellation and 50h battery life with Bazaar Pro Wireless Headphones. Buy online with fast delivery on BazaarHub.")
    private String seoDescription;

    @Schema(description = "Content lifecycle status (Always generated as DRAFT requiring human review; never published automatically)", example = "DRAFT")
    @Builder.Default
    private String status = "DRAFT";

    @Schema(description = "Model used for content generation", example = "openai/gpt-oss-20b")
    private String modelName;
}
