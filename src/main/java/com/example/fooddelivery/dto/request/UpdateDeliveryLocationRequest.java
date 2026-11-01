package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryLocationRequest(
        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        Double heading,

        Double speed,

        Double distanceRemainingKm,

        Integer estimatedMinutes
) {}

