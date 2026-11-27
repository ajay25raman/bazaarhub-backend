package com.bazaarhub.backend.wishlist.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer wishlist rename request")
public class WishlistUpdateRequest {

    @NotBlank(message = "Wishlist name is required")
    @Size(max = 150, message = "Wishlist name must not exceed 150 characters")
    @Schema(description = "Updated wishlist display name", example = "Birthday Gift Ideas")
    private String name;
}
