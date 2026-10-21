package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for adding a menu item to the customer's cart.
 * <p>
 * Notice: Price and restaurant are never supplied by the client;
 * they are verified and extracted from the database.
 * </p>
 */
public record AddToCartRequest(
        @NotNull(message = "Menu item ID is required")
        Long menuItemId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 50, message = "Quantity cannot exceed 50")
        Integer quantity
) {}
