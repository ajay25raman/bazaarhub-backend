package com.bazaarhub.backend.catalog.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product image metadata response")
public class ProductImageResponse {

    @Schema(description = "Image record ID", example = "101")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Publicly accessible image URL", example = "https://cdn.bazaarhub.com/products/iphone15-front.webp")
    private String imageUrl;

    @Schema(description = "Alt text for accessibility", example = "Front view of iPhone 15 Pro Max")
    private String altText;

    @Schema(description = "Display order index", example = "0")
    private Integer displayOrder;

    @JsonProperty("isPrimary")
    @Schema(description = "Whether this is the primary hero image", example = "true")
    private boolean isPrimary;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:00:00Z")
    private Instant updatedAt;
}
