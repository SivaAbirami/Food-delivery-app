package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateOfferRequest(
        @NotBlank(message = "Coupon code is required")
        String code,

        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Discount percentage is required")
        @Min(value = 1, message = "Discount percentage must be at least 1")
        @Max(value = 100, message = "Discount percentage cannot exceed 100")
        Integer discountPercentage,

        BigDecimal maxDiscountAmount,

        BigDecimal minOrderAmount,

        String bannerImageUrl,

        String gradientColor
) {}

