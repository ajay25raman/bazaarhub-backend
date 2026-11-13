package com.bazaarhub.backend.seller.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storefront update request payload")
public class StoreUpdateRequest {

    @Size(min = 2, max = 200, message = "Store name must be between 2 and 200 characters")
    @Schema(description = "Customer-facing storefront display name", example = "Acme Premier Store")
    private String storeName;

    @Schema(description = "Storefront description and operating information", example = "Premier vendor of electronics and household goods.")
    private String description;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    @Schema(description = "Store logo image URL", example = "https://cdn.bazaarhub.com/logos/acme-new-logo.png")
    private String logoUrl;

    @Size(max = 500, message = "Banner URL must not exceed 500 characters")
    @Schema(description = "Store hero banner image URL", example = "https://cdn.bazaarhub.com/banners/acme-new-banner.jpg")
    private String bannerUrl;
}
