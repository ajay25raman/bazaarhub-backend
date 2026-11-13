package com.bazaarhub.backend.catalog.application.dto.response;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hierarchical category tree node")
public class CategoryTreeResponse {

    @Schema(description = "Category ID", example = "1")
    private Long id;

    @Schema(description = "Category display name", example = "Electronics")
    private String name;

    @Schema(description = "Unique URL slug", example = "electronics")
    private String slug;

    @Schema(description = "Category description", example = "Consumer electronics and appliances.")
    private String description;

    @Schema(description = "Category image URL", example = "https://cdn.bazaarhub.com/categories/electronics.png")
    private String imageUrl;

    @Schema(description = "Category status", example = "ACTIVE")
    private CatalogStatus status;

    @Schema(description = "Display/sorting order", example = "1")
    private Integer displayOrder;

    @Schema(description = "Parent category ID (null for root level)", example = "null")
    private Long parentId;

    @Schema(description = "Nested child subcategories")
    @Builder.Default
    private List<CategoryTreeResponse> children = new ArrayList<>();
}
