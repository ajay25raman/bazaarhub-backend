package com.bazaarhub.backend.wishlist.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Check whether a product exists in customer wishlist")
public class WishlistProductExistsResponse {

    @Schema(description = "Catalog Product ID", example = "105")
    private Long productId;

    @Schema(description = "Whether product is present in wishlist", example = "true")
    private boolean inWishlist;
}
