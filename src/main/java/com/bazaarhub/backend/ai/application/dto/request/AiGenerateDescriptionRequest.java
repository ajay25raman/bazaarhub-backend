package com.bazaarhub.backend.ai.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to generate rich catalog descriptions and SEO metadata using Groq AI")
public class AiGenerateDescriptionRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Schema(description = "Base product name", example = "Bazaar Pro Noise Cancelling Wireless Headphones")
    private String productName;

    @Schema(description = "Category name or taxonomy context", example = "Electronics > Audio > Headphones")
    private String categoryName;

    @Schema(description = "Optional Brand name", example = "BazaarSound")
    private String brandName;

    @Schema(description = "List of key technical specifications, features, or selling points", example = "[\"40dB Active Noise Cancellation\", \"50-hour battery life\", \"Bluetooth 5.4\", \"Fast Type-C charging\"]")
    private List<String> keyFeatures;

    @Schema(description = "Intended target audience or tone (e.g., audiophiles, casual listeners, gamers, professionals)", example = "Frequent flyers and daily office commuters")
    private String targetAudience;
}
