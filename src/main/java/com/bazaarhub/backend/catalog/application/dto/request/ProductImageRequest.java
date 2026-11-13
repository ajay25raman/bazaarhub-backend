package com.bazaarhub.backend.catalog.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product image metadata request")
public class ProductImageRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Schema(description = "Publicly accessible image URL", example = "https://cdn.bazaarhub.com/products/iphone15-front.webp")
    private String imageUrl;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    @Schema(description = "Descriptive alt text for accessibility and SEO", example = "Front view of iPhone 15 Pro Max in Natural Titanium")
    private String altText;

    @Schema(description = "Display order index (lower values rendered first; default 0)", example = "0")
    private Integer displayOrder;

    @Schema(description = "Flag indicating whether this is the primary hero image (default false)", example = "true")
    private Boolean isPrimary;
}
