package com.example.fooddelivery.dto.response;

import java.math.BigDecimal;

/**
 * Public response representation of an individual item in a customer's cart.
 */
public record CartItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        String imageUrl
) {}
