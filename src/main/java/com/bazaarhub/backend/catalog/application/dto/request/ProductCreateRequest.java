package com.bazaarhub.backend.catalog.application.dto.request;

import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
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
@Schema(description = "Seller product creation request payload")
public class ProductCreateRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Schema(description = "Product display title", example = "iPhone 15 Pro Max 256GB")
    private String name;

    @Size(max = 280, message = "Slug must not exceed 280 characters")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    @Schema(description = "Unique URL slug (optional; auto-generated from name if omitted)", example = "iphone-15-pro-max-256gb")
    private String slug;

    @NotNull(message = "Category ID is required")
    @Schema(description = "Associated category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Associated brand ID (optional)", example = "1")
    private Long brandId;

    @Schema(description = "Comprehensive product description and specifications", example = "Titanium design with A17 Pro chip and 48MP camera system.")
    private String description;

    @Schema(description = "Initial product lifecycle status (DRAFT or ACTIVE; default is DRAFT)", example = "DRAFT")
    private ProductStatus status;
}
