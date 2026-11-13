package com.bazaarhub.backend.catalog.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Seller product update request payload")
public class ProductUpdateRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Schema(description = "Product display title", example = "iPhone 15 Pro Max 256GB - Natural Titanium")
    private String name;

    @Size(max = 280, message = "Slug must not exceed 280 characters")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    @Schema(description = "Unique URL slug (optional; preserves current slug if omitted or blank)", example = "iphone-15-pro-max-256gb-natural")
    private String slug;

    @NotNull(message = "Category ID is required")
    @Schema(description = "Associated category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Associated brand ID (optional)", example = "1")
    private Long brandId;

    @Schema(description = "Product description", example = "Updated product description and warranty details.")
    private String description;
}
