package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for customer checkout from active shopping cart.
 */
public record CheckoutRequest(
        @NotBlank(message = "Delivery address is required")
        @Size(max = 500, message = "Delivery address must not exceed 500 characters")
        String deliveryAddress,

        @Size(max = 500, message = "Special instructions must not exceed 500 characters")
        String specialInstructions,

        @Size(max = 128, message = "Idempotency key must not exceed 128 characters")
        String idempotencyKey
) {}
