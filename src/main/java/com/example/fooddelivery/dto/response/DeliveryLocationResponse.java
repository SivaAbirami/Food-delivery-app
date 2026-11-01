package com.example.fooddelivery.dto.response;

public record DeliveryLocationResponse(
        Long orderId,
        String driverName,
        String driverPhone,
        Double latitude,
        Double longitude,
        Double heading,
        Double speed,
        Double distanceRemainingKm,
        Integer estimatedMinutes,
        String orderStatus,
        String restaurantName,
        String restaurantAddress,
        Double restaurantLat,
        Double restaurantLng,
        String deliveryAddress,
        Double destinationLat,
        Double destinationLng,
        Long lastUpdatedAt
) {}

