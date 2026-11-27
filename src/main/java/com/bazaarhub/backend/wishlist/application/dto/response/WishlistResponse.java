package com.bazaarhub.backend.wishlist.application.dto.response;

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
@Schema(description = "Customer wishlist metadata response")
public class WishlistResponse {

    @Schema(description = "Wishlist ID", example = "10")
    private Long id;

    @Schema(description = "Owner User ID", example = "42")
    private Long userId;

    @Schema(description = "Wishlist name", example = "My Wishlist")
    private String name;

    @Schema(description = "Total number of products in wishlist", example = "5")
    private int totalItems;

    @Schema(description = "Creation timestamp", example = "2026-03-01T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-01T12:30:00Z")
    private Instant updatedAt;
}
