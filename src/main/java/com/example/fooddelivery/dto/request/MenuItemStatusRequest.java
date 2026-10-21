package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for toggling a menu item's availability status.
 */
public record MenuItemStatusRequest(
        @NotNull(message = "isAvailable status flag is required")
        Boolean isAvailable
) {}
