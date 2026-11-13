package com.bazaarhub.backend.catalog.application.dto.request;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Category creation request payload")
public class CategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 150, message = "Category name must be between 2 and 150 characters")
    @Schema(description = "Category display name", example = "Electronics & Gadgets")
    private String name;

    @Size(max = 180, message = "Slug must not exceed 180 characters")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    @Schema(description = "Unique URL slug (optional; auto-generated from name if omitted)", example = "electronics-gadgets")
    private String slug;

    @Schema(description = "Parent category ID (optional; null for root categories)", example = "1")
    private Long parentId;

    @Schema(description = "Category description", example = "Consumer electronics, smartphones, computing devices and audio gear.")
    private String description;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Schema(description = "Category hero/banner image URL", example = "https://cdn.bazaarhub.com/categories/electronics.png")
    private String imageUrl;

    @Schema(description = "Sorting/display priority (lower numbers appear first)", example = "10")
    private Integer displayOrder;

    @Schema(description = "Initial category status", example = "ACTIVE")
    private CatalogStatus status;
}
