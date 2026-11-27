package com.bazaarhub.backend.order.application.mapper;

import com.bazaarhub.backend.customer.application.mapper.CustomerMapper;
import com.bazaarhub.backend.order.application.dto.response.*;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderItemResponse toOrderItemResponse(OrderItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return OrderItemResponse.builder()
                .id(entity.getId())
                .variantId(entity.getVariant() != null ? entity.getVariant().getId() : null)
                .productName(entity.getProductName())
                .variantSku(entity.getVariantSku())
                .variantAttributes(entity.getVariantAttributes())
                .unitPrice(entity.getUnitPrice())
                .mrp(entity.getMrp())
                .taxRate(entity.getTaxRate())
                .taxAmount(entity.getTaxAmount())
                .quantity(entity.getQuantity())
                .totalPrice(entity.getTotalPrice())
                .build();
    }

    public static OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return OrderStatusHistoryResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .sellerOrderId(entity.getSellerOrder() != null ? entity.getSellerOrder().getId() : null)
                .previousStatus(entity.getPreviousStatus())
                .newStatus(entity.getNewStatus())
                .changedByUserId(entity.getChangedBy() != null ? entity.getChangedBy().getId() : null)
                .changedByUserName(entity.getChangedBy() != null ? entity.getChangedBy().getFullName() : null)
                .remarks(entity.getRemarks())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static SellerOrderResponse toSellerOrderResponse(SellerOrderEntity entity) {
        return toSellerOrderResponse(entity, null, Collections.emptyList());
    }

    public static SellerOrderResponse toSellerOrderResponse(SellerOrderEntity entity, List<OrderStatusHistoryEntity> history) {
        return toSellerOrderResponse(entity, null, history);
    }

    public static SellerOrderResponse toSellerOrderResponse(SellerOrderEntity entity,
                                                            List<OrderItemEntity> items,
                                                            List<OrderStatusHistoryEntity> history) {
        if (entity == null) {
            return null;
        }

        List<OrderItemEntity> effectiveItems = items != null ? items : (entity.getItems() != null ? entity.getItems() : Collections.emptyList());
        List<OrderItemResponse> itemResponses = effectiveItems.stream()
                .map(OrderMapper::toOrderItemResponse)
                .collect(Collectors.toList());

        List<OrderStatusHistoryResponse> historyResponses = history != null
                ? history.stream().map(OrderMapper::toOrderStatusHistoryResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return SellerOrderResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .orderNumber(entity.getOrder() != null ? entity.getOrder().getOrderNumber() : null)
                .sellerId(entity.getSeller() != null ? entity.getSeller().getId() : null)
                .sellerBusinessName(entity.getSeller() != null ? entity.getSeller().getBusinessName() : null)
                .subOrderNumber(entity.getSubOrderNumber())
                .status(entity.getStatus())
                .subtotal(entity.getSubtotal())
                .taxAmount(entity.getTaxAmount())
                .deliveryFee(entity.getDeliveryFee())
                .totalAmount(entity.getTotalAmount())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .shippingAddress(entity.getOrder() != null ? CustomerMapper.toAddressResponse(entity.getOrder().getShippingAddress()) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(itemResponses)
                .statusHistory(historyResponses)
                .build();
    }

    public static OrderResponse toOrderResponse(OrderEntity entity) {
        return toOrderResponse(entity, Collections.emptyList(), Collections.emptyMap());
    }

    public static OrderResponse toOrderResponse(OrderEntity entity,
                                               List<OrderStatusHistoryEntity> orderHistory,
                                               Map<Long, List<OrderStatusHistoryEntity>> sellerHistoryMap) {
        return toOrderResponse(entity, null, null, orderHistory, sellerHistoryMap);
    }

    public static OrderResponse toOrderResponse(OrderEntity entity,
                                               List<SellerOrderEntity> sellerOrders,
                                               Map<Long, List<OrderItemEntity>> sellerItemsMap,
                                               List<OrderStatusHistoryEntity> orderHistory,
                                               Map<Long, List<OrderStatusHistoryEntity>> sellerHistoryMap) {
        if (entity == null) {
            return null;
        }

        List<SellerOrderEntity> effectiveSellerOrders = sellerOrders != null ? sellerOrders : (entity.getSellerOrders() != null ? entity.getSellerOrders() : Collections.emptyList());

        List<SellerOrderResponse> sellerOrderResponses = effectiveSellerOrders.stream().map(so -> {
            List<OrderItemEntity> items = sellerItemsMap != null ? sellerItemsMap.getOrDefault(so.getId(), Collections.emptyList()) : so.getItems();
            List<OrderStatusHistoryEntity> soHistory = sellerHistoryMap != null
                    ? sellerHistoryMap.getOrDefault(so.getId(), Collections.emptyList())
                    : Collections.emptyList();
            return toSellerOrderResponse(so, items, soHistory);
        }).collect(Collectors.toList());

        List<OrderStatusHistoryResponse> orderHistoryResponses = orderHistory != null
                ? orderHistory.stream().map(OrderMapper::toOrderStatusHistoryResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return OrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .customerId(entity.getUser() != null ? entity.getUser().getId() : null)
                .customerName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .customerEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .shippingAddress(CustomerMapper.toAddressResponse(entity.getShippingAddress()))
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .currency(entity.getCurrency())
                .subtotal(entity.getSubtotal())
                .taxAmount(entity.getTaxAmount())
                .deliveryFee(entity.getDeliveryFee())
                .discountAmount(entity.getDiscountAmount())
                .grandTotal(entity.getGrandTotal())
                .specialInstructions(entity.getSpecialInstructions())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .sellerOrders(sellerOrderResponses)
                .statusHistory(orderHistoryResponses)
                .build();
    }

    public static OrderSummaryResponse toOrderSummaryResponse(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        int totalItems = 0;
        int sellerOrdersCount = 0;
        if (entity.getSellerOrders() != null) {
            sellerOrdersCount = entity.getSellerOrders().size();
            for (SellerOrderEntity so : entity.getSellerOrders()) {
                if (so.getItems() != null) {
                    for (OrderItemEntity item : so.getItems()) {
                        totalItems += (item.getQuantity() != null ? item.getQuantity() : 0);
                    }
                }
            }
        }

        return OrderSummaryResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .customerId(entity.getUser() != null ? entity.getUser().getId() : null)
                .customerName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .grandTotal(entity.getGrandTotal())
                .totalItems(totalItems)
                .sellerOrdersCount(sellerOrdersCount)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
