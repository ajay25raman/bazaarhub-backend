package com.bazaarhub.backend.search.domain.model;

import java.util.Arrays;

/**
 * Strict allowlist of safe sort options supported for public product search and discovery.
 */
public enum ProductSortOption {
    RELEVANCE("relevance"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc"),
    NEWEST("newest"),
    NAME_ASC("name_asc"),
    NAME_DESC("name_desc");

    private final String value;

    ProductSortOption(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Safely parses user input into a ProductSortOption.
     * Returns the provided default option if the input is null, blank, or unrecognized.
     */
    public static ProductSortOption fromString(String text, ProductSortOption defaultOption) {
        if (text == null || text.isBlank()) {
            return defaultOption != null ? defaultOption : NEWEST;
        }

        String normalized = text.trim().toLowerCase().replace(",", "_");
        for (ProductSortOption option : values()) {
            if (option.name().equalsIgnoreCase(normalized) || option.value.equalsIgnoreCase(normalized)) {
                return option;
            }
        }
        // Graceful fallback for unrecognized sort parameters
        return defaultOption != null ? defaultOption : NEWEST;
    }
}
