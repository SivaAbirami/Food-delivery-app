package com.example.fooddelivery.dto.response;

import java.math.BigDecimal;

public record OfferResponse(
        Long id,
        String code,
        String title,
        String description,
        Integer discountPercentage,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        Long restaurantId,
        String restaurantName,
        String bannerImageUrl,
        String gradientColor,
        boolean isActive
) {}

