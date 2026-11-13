package com.bazaarhub.backend.catalog.application.dto.response;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
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
@Schema(description = "Category representation")
public class CategoryResponse {

    @Schema(description = "Category ID", example = "1")
    private Long id;

    @Schema(description = "Category display name", example = "Electronics & Gadgets")
    private String name;

    @Schema(description = "Unique URL slug", example = "electronics-gadgets")
    private String slug;

    @Schema(description = "Category description", example = "Consumer electronics, smartphones and computing devices.")
    private String description;

    @Schema(description = "Category hero/banner image URL", example = "https://cdn.bazaarhub.com/categories/electronics.png")
    private String imageUrl;

    @Schema(description = "Category status", example = "ACTIVE")
    private CatalogStatus status;

    @Schema(description = "Display/sorting order", example = "10")
    private Integer displayOrder;

    @Schema(description = "Parent category ID (null if root category)", example = "null")
    private Long parentId;

    @Schema(description = "Parent category display name", example = "null")
    private String parentName;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:30:00Z")
    private Instant updatedAt;
}
