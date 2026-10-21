package com.example.fooddelivery.dto.request;

import com.example.fooddelivery.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for transitioning order status by Restaurant Owner or Delivery Partner.
 */
public record OrderStatusUpdateRequest(
        @NotNull(message = "Target order status is required")
        OrderStatus status
) {}
