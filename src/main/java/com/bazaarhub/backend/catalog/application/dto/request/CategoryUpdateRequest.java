package com.bazaarhub.backend.catalog.application.dto.request;

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
@Schema(description = "Category update request payload")
public class CategoryUpdateRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 150, message = "Category name must be between 2 and 150 characters")
    @Schema(description = "Category display name", example = "Consumer Electronics")
    private String name;

    @Size(max = 180, message = "Slug must not exceed 180 characters")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    @Schema(description = "Unique URL slug (optional; preserves current slug or regenerates if blank)", example = "consumer-electronics")
    private String slug;

    @Schema(description = "Parent category ID (optional; pass null to convert to root category)", example = "2")
    private Long parentId;

    @Schema(description = "Category description", example = "Updated description for electronics category.")
    private String description;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Schema(description = "Category hero/banner image URL", example = "https://cdn.bazaarhub.com/categories/electronics-updated.png")
    private String imageUrl;

    @Schema(description = "Sorting/display priority (lower numbers appear first)", example = "5")
    private Integer displayOrder;
}
