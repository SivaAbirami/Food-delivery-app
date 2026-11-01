package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ValidateCouponRequest(
        @NotBlank(message = "Coupon code is required")
        String code,

        Long restaurantId,

        @NotNull(message = "Order subtotal is required")
        BigDecimal subtotal
) {}

