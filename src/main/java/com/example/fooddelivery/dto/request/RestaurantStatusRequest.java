package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for toggling restaurant operational status (open/closed).
 */
public record RestaurantStatusRequest(
        @NotNull(message = "isOpen status flag is required")
        Boolean isOpen
) {}
