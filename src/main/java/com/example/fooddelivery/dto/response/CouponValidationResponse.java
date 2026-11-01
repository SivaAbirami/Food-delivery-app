package com.example.fooddelivery.dto.response;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean isValid,
        String message,
        String code,
        BigDecimal discountAmount,
        BigDecimal finalTotal
) {}

