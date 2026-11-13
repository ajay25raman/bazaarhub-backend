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
@Schema(description = "Brand representation")
public class BrandResponse {

    @Schema(description = "Brand ID", example = "1")
    private Long id;

    @Schema(description = "Brand name", example = "Apple")
    private String name;

    @Schema(description = "Unique URL slug", example = "apple")
    private String slug;

    @Schema(description = "Brand logo image URL", example = "https://cdn.bazaarhub.com/brands/apple-logo.png")
    private String logoUrl;

    @Schema(description = "Brand status", example = "ACTIVE")
    private CatalogStatus status;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:30:00Z")
    private Instant updatedAt;
}
