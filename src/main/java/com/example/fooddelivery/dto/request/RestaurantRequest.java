package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating restaurant information.
 * <p>
 * Notice: 'ownerId' and 'isOpen' are intentionally excluded.
 * The owner is derived from the authenticated principal and status changes
 * are managed via the dedicated PATCH /api/restaurants/{id}/status endpoint.
 * </p>
 */
public record RestaurantRequest(
        @NotBlank(message = "Restaurant name is required")
        @Size(max = 120, message = "Restaurant name must not exceed 120 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotBlank(message = "Phone number is required")
        @Size(min = 7, max = 20, message = "Phone number must be between 7 and 20 characters")
        String phone
) {}
