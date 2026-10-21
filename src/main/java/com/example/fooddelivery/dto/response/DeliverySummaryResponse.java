package com.example.fooddelivery.dto.response;

import com.example.fooddelivery.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Summary DTO tailored specifically for Delivery Partner workflows.
 * Contains restaurant pickup details, customer delivery location, and order metadata.
 */
public record DeliverySummaryResponse(
        Long orderId,
        String orderNumber,
        Long restaurantId,
        String restaurantName,
        String restaurantAddress,
        String restaurantPhone,
        String deliveryAddress,
        String specialInstructions,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {}
