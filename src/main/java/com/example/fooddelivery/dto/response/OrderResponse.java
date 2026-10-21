package com.example.fooddelivery.dto.response;

import com.example.fooddelivery.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detailed response payload representing an Order.
 */
public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        Long restaurantId,
        String restaurantName,
        Long deliveryPartnerId,
        OrderStatus status,
        String deliveryAddress,
        String specialInstructions,
        List<OrderItemResponse> items,
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {
    // Constructor ensuring clean record instantiation
    public OrderResponse(
            Long id,
            String orderNumber,
            Long customerId,
            Long restaurantId,
            String restaurantName,
            Long deliveryPartnerId,
            OrderStatus status,
            String deliveryAddress,
            String specialInstructions,
            List<OrderItemResponse> items,
            BigDecimal subtotalAmount,
            BigDecimal deliveryFee,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.deliveryPartnerId = deliveryPartnerId;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.specialInstructions = specialInstructions;
        this.items = items != null ? items : List.of();
        this.subtotalAmount = subtotalAmount;
        this.deliveryFee = deliveryFee;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
