package com.example.fooddelivery.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Public response representation of a customer's shopping cart.
 */
public record CartResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        List<CartItemResponse> items,
        int totalItemCount,
        BigDecimal totalAmount,
        Instant updatedAt
) {}
