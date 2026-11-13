package com.bazaarhub.backend.seller.application.dto.request;

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
@Schema(description = "Storefront creation request payload")
public class StoreCreateRequest {

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 200, message = "Store name must be between 2 and 200 characters")
    @Schema(description = "Customer-facing storefront display name", example = "Acme Official Store")
    private String storeName;

    @Size(max = 220, message = "Slug must not exceed 220 characters")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    @Schema(description = "Unique URL slug (optional; auto-generated from store name if omitted)", example = "acme-official-store")
    private String slug;

    @Schema(description = "Public storefront description and policies", example = "Official verified store for Acme retail goods and electronics.")
    private String description;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    @Schema(description = "Store logo image URL", example = "https://cdn.bazaarhub.com/logos/acme-logo.png")
    private String logoUrl;

    @Size(max = 500, message = "Banner URL must not exceed 500 characters")
    @Schema(description = "Store hero banner image URL", example = "https://cdn.bazaarhub.com/banners/acme-banner.jpg")
    private String bannerUrl;
}
