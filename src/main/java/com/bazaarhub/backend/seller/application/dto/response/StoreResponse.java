package com.bazaarhub.backend.seller.application.dto.response;

import com.bazaarhub.backend.seller.domain.model.StoreStatus;
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
@Schema(description = "Storefront representation")
public class StoreResponse {

    @Schema(description = "Store ID", example = "5")
    private Long id;

    @Schema(description = "Owner seller ID", example = "10")
    private Long sellerId;

    @Schema(description = "Store display name", example = "Acme Official Store")
    private String storeName;

    @Schema(description = "Unique URL slug", example = "acme-official-store")
    private String slug;

    @Schema(description = "Storefront description", example = "Official verified store for Acme retail goods.")
    private String description;

    @Schema(description = "Store logo image URL", example = "https://cdn.bazaarhub.com/logos/acme-logo.png")
    private String logoUrl;

    @Schema(description = "Store hero banner image URL", example = "https://cdn.bazaarhub.com/banners/acme-banner.jpg")
    private String bannerUrl;

    @Schema(description = "Current storefront status", example = "INACTIVE")
    private StoreStatus status;

    @Schema(description = "Store creation timestamp", example = "2026-11-13T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "Store last updated timestamp", example = "2026-11-13T10:45:00Z")
    private Instant updatedAt;
}
