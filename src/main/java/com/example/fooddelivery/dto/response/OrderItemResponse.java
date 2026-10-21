package com.example.fooddelivery.dto.response;

import java.math.BigDecimal;

/**
 * Response payload representing an immutable item snapshot in an order.
 */
public record OrderItemResponse(
        Long id,
        Long menuItemId,
        String itemName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
) {}
