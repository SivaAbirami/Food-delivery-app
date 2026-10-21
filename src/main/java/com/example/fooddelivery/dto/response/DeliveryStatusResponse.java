package com.example.fooddelivery.dto.response;

import com.example.fooddelivery.enums.OrderStatus;

import java.time.Instant;

/**
 * Public delivery status response payload for customer and owner order tracking.
 * Strictly excludes sensitive internal credentials, tokens, or private driver data.
 */
public record DeliveryStatusResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        boolean deliveryPartnerAssigned,
        String deliveryPartnerDisplayName,
        String restaurantName,
        String deliveryAddress,
        Instant updatedAt
) {}
