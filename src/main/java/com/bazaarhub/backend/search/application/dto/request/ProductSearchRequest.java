package com.bazaarhub.backend.search.application.dto.request;

import com.bazaarhub.backend.search.domain.exception.InvalidSearchFilterException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request criteria for public storefront product search and filtering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criteria parameters for public product search and discovery")
public class ProductSearchRequest {

    @Schema(description = "Keyword search query (matches product title, description, and SKU)", example = "smartphone")
    private String q;

    @Schema(description = "Alternative keyword alias", example = "smartphone")
    private String keyword;

    @Schema(description = "Category ID filter", example = "1")
    private Long categoryId;

    @Schema(description = "Subcategory ID filter", example = "2")
    private Long subcategoryId;

    @Schema(description = "Brand ID filter", example = "1")
    private Long brandId;

    @Schema(description = "Seller ID filter", example = "1")
    private Long sellerId;

    @Min(value = 0, message = "Minimum price cannot be negative")
    @Schema(description = "Minimum selling price filter", example = "500.00")
    private BigDecimal minPrice;

    @Min(value = 0, message = "Maximum price cannot be negative")
    @Schema(description = "Maximum selling price filter", example = "200000.00")
    private BigDecimal maxPrice;

    @Schema(description = "In-stock availability filter (true = in-stock only, false = out-of-stock only)", example = "true")
    private Boolean inStock;

    @Schema(description = "Sorting criteria (relevance, price_asc, price_desc, newest, name_asc, name_desc)", example = "relevance")
    private String sort;

    @Min(value = 0, message = "Page index must not be less than zero")
    @Builder.Default
    @Schema(description = "Zero-based page number", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    @Schema(description = "Page size (1 - 100)", example = "20", defaultValue = "20")
    private Integer size = 20;

    /**
     * Resolves the keyword search query from either `q` or `keyword`.
     */
    public String getResolvedQuery() {
        if (q != null && !q.isBlank()) {
            return q.trim();
        }
        if (keyword != null && !keyword.isBlank()) {
            return keyword.trim();
        }
        return null;
    }

    /**
     * Validates logical constraints (e.g., price boundaries and page limits).
     */
    public void validate() {
        if (page != null && page < 0) {
            throw new InvalidSearchFilterException("Page index must not be less than zero");
        }
        if (size != null && size > 100) {
            throw new InvalidSearchFilterException("Page size cannot exceed 100");
        }
        if (size != null && size < 1) {
            throw new InvalidSearchFilterException("Page size must be at least 1");
        }
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSearchFilterException("Minimum price cannot be negative");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSearchFilterException("Maximum price cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidSearchFilterException("Minimum price cannot be greater than maximum price");
        }
    }
}
