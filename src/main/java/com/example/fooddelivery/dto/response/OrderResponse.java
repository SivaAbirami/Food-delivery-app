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
        String deliveryOtp,
        Instant createdAt,
        Instant updatedAt
) {
    // Backward-compatible constructor
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
        this(id, orderNumber, customerId, restaurantId, restaurantName, deliveryPartnerId, status,
                deliveryAddress, specialInstructions, items, subtotalAmount, deliveryFee, taxAmount, totalAmount, null, createdAt, updatedAt);
    }
}
