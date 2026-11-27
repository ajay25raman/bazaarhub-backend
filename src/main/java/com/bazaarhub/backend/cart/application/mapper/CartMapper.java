package com.bazaarhub.backend.cart.application.mapper;

import com.bazaarhub.backend.cart.application.dto.response.CartItemResponse;
import com.bazaarhub.backend.cart.application.dto.response.CartResponse;
import com.bazaarhub.backend.cart.application.dto.response.CartSummaryResponse;
import com.bazaarhub.backend.cart.application.dto.response.SellerCartGroupDto;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartItemEntity;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper transforming CartEntity and CartItemEntity into safe, customer-facing response DTOs.
 * Computes exact monetary subtotals, tax allocations, and seller-wise groupings using BigDecimal.
 */
public final class CartMapper {

    private CartMapper() {
    }

    public static CartItemResponse toCartItemResponse(CartItemEntity item, int availableStock) {
        if (item == null) {
            return null;
        }

        ProductVariantEntity variant = item.getVariant();
        ProductEntity product = variant != null ? variant.getProduct() : null;
        SellerEntity seller = product != null ? product.getSeller() : null;

        BigDecimal unitPrice = (variant != null && variant.getSellingPrice() != null)
                ? variant.getSellingPrice().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal mrp = (variant != null && variant.getMrp() != null)
                ? variant.getMrp().setScale(2, RoundingMode.HALF_UP)
                : unitPrice;

        int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        // Resolve Primary Image URL
        String primaryImageUrl = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImageUrl = product.getImages().stream()
                    .filter(ProductImageEntity::isPrimary)
                    .findFirst()
                    .map(ProductImageEntity::getImageUrl)
                    .orElseGet(() -> product.getImages().get(0).getImageUrl());
        }

        boolean hasStock = availableStock >= quantity;

        boolean isPurchasable = variant != null && variant.getStatus() == CatalogStatus.ACTIVE
                && product != null && product.getStatus() == ProductStatus.ACTIVE
                && product.getApprovalStatus() == ProductApprovalStatus.APPROVED
                && seller != null && seller.getApprovalStatus() == SellerApprovalStatus.APPROVED;

        return CartItemResponse.builder()
                .itemId(item.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productSlug(product != null ? product.getSlug() : null)
                .variantId(variant != null ? variant.getId() : null)
                .sku(variant != null ? variant.getSku() : null)
                .variantName(variant != null ? variant.getVariantName() : null)
                .attributes(variant != null ? variant.getAttributes() : null)
                .primaryImageUrl(primaryImageUrl)
                .sellerId(seller != null ? seller.getId() : null)
                .sellerBusinessName(seller != null ? seller.getBusinessName() : null)
                .unitPrice(unitPrice)
                .mrp(mrp)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .inStock(hasStock)
                .availableStock(availableStock)
                .priceChanged(false)
                .purchasable(isPurchasable)
                .build();
    }

    public static CartResponse toCartResponse(CartEntity cart, Map<Long, Integer> variantStockMap) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> itemResponses = new ArrayList<>();
        if (cart.getItems() != null) {
            for (CartItemEntity item : cart.getItems()) {
                Long variantId = item.getVariant() != null ? item.getVariant().getId() : null;
                int stock = (variantId != null && variantStockMap != null)
                        ? variantStockMap.getOrDefault(variantId, 0)
                        : 0;
                itemResponses.add(toCartItemResponse(item, stock));
            }
        }

        // Calculate Multi-Vendor Groupings and Totals
        Map<Long, List<CartItemResponse>> sellerGroups = itemResponses.stream()
                .filter(i -> i.getSellerId() != null)
                .collect(Collectors.groupingBy(CartItemResponse::getSellerId, LinkedHashMap::new, Collectors.toList()));

        List<SellerCartGroupDto> sellers = new ArrayList<>();
        BigDecimal cartSubtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal cartTax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Map.Entry<Long, List<CartItemResponse>> entry : sellerGroups.entrySet()) {
            List<CartItemResponse> sItems = entry.getValue();
            String sellerName = !sItems.isEmpty() ? sItems.get(0).getSellerBusinessName() : "Unknown Merchant";

            BigDecimal sellerSubtotal = sItems.stream()
                    .map(CartItemResponse::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            int sItemCount = sItems.stream().mapToInt(CartItemResponse::getQuantity).sum();

            sellers.add(SellerCartGroupDto.builder()
                    .sellerId(entry.getKey())
                    .businessName(sellerName)
                    .itemCount(sItemCount)
                    .subtotal(sellerSubtotal)
                    .items(sItems)
                    .build());

            cartSubtotal = cartSubtotal.add(sellerSubtotal);
        }

        // Calculate Tax based on configured tax rates on active variants
        if (cart.getItems() != null) {
            for (CartItemEntity item : cart.getItems()) {
                if (item.getVariant() != null && item.getVariant().getTaxRate() != null && item.getVariant().getSellingPrice() != null) {
                    BigDecimal taxRate = item.getVariant().getTaxRate();
                    BigDecimal linePrice = item.getVariant().getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal itemTax = linePrice.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    cartTax = cartTax.add(itemTax);
                }
            }
        }

        int totalQuantity = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal deliveryFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = cartSubtotal.add(cartTax).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        return CartResponse.builder()
                .cartId(cart.getId())
                .status(cart.getStatus())
                .totalItems(totalQuantity)
                .totalUniqueItems(itemResponses.size())
                .subtotal(cartSubtotal)
                .taxAmount(cartTax)
                .deliveryFee(deliveryFee)
                .grandTotal(grandTotal)
                .sellers(sellers)
                .items(itemResponses)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public static CartSummaryResponse toSummaryResponse(CartResponse cartResponse) {
        if (cartResponse == null) {
            return CartSummaryResponse.builder()
                    .totalItems(0)
                    .totalUniqueItems(0)
                    .subtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .taxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .deliveryFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .grandTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .sellers(Collections.emptyList())
                    .build();
        }

        List<CartSummaryResponse.SellerSubtotalDto> sellerSummaries = cartResponse.getSellers().stream()
                .map(s -> CartSummaryResponse.SellerSubtotalDto.builder()
                        .sellerId(s.getSellerId())
                        .businessName(s.getBusinessName())
                        .itemCount(s.getItemCount())
                        .subtotal(s.getSubtotal())
                        .build())
                .toList();

        return CartSummaryResponse.builder()
                .totalItems(cartResponse.getTotalItems())
                .totalUniqueItems(cartResponse.getTotalUniqueItems())
                .subtotal(cartResponse.getSubtotal())
                .taxAmount(cartResponse.getTaxAmount())
                .deliveryFee(cartResponse.getDeliveryFee())
                .grandTotal(cartResponse.getGrandTotal())
                .sellers(sellerSummaries)
                .build();
    }
}
