package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for creating or updating a menu item.
 * <p>
 * Notice: 'id', 'restaurantId', 'ownerId', and 'isAvailable' are intentionally excluded.
 * The restaurant is obtained from the URL path, the owner from JWT security context,
 * and availability is toggled via the dedicated status endpoint.
 * </p>
 */
public record MenuItemRequest(
        @NotBlank(message = "Menu item name is required")
        @Size(max = 120, message = "Menu item name must not exceed 120 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Price must be at least 0.01")
        @Digits(integer = 8, fraction = 2, message = "Price format must have at most 8 integer digits and 2 decimal places")
        BigDecimal price,

        @NotBlank(message = "Category is required")
        @Size(max = 60, message = "Category must not exceed 60 characters")
        String category,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl
) {}
