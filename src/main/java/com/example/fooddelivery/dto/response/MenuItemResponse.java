package com.example.fooddelivery.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public response representation of a menu item entity.
 */
public record MenuItemResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        String name,
        String description,
        BigDecimal price,
        String category,
        boolean isAvailable,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt
) {}
