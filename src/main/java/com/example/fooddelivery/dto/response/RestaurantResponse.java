package com.example.fooddelivery.dto.response;

import java.time.Instant;

/**
 * Public response representation of a restaurant entity.
 */
public record RestaurantResponse(
        Long id,
        String name,
        String description,
        String address,
        String phone,
        boolean isOpen,
        Long ownerId,
        String ownerName,
        Instant createdAt
) {}
