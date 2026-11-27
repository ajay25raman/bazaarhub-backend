package com.bazaarhub.backend.search.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Metadata facets describing available filters for the active search result set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search filter facets and boundary values")
public class ProductSearchFilterFacetsDto {

    @Schema(description = "Lowest price found in search results", example = "499.00")
    private BigDecimal minPrice;

    @Schema(description = "Highest price found in search results", example = "199900.00")
    private BigDecimal maxPrice;

    @Schema(description = "Categories present in matching products")
    private List<CategoryFacet> categories;

    @Schema(description = "Brands present in matching products")
    private List<BrandFacet> brands;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Category facet item")
    public static class CategoryFacet {
        private Long id;
        private String name;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Brand facet item")
    public static class BrandFacet {
        private Long id;
        private String name;
        private long count;
    }
}
