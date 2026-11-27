package com.bazaarhub.backend.wishlist.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add product to customer wishlist")
public class AddWishlistItemRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the catalog product to save to wishlist", example = "105")
    private Long productId;
}
