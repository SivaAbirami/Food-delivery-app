package com.example.fooddelivery.enums;

/**
 * Lifecycle states of a food delivery order.
 */
public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
