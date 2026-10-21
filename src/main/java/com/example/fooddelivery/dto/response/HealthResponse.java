package com.example.fooddelivery.dto.response;

import java.time.Instant;

/**
 * Immutable response DTO representing the application's health status.
 *
 * @param status      Current operational status (e.g., "UP")
 * @param timestamp   UTC timestamp when the health check was performed
 * @param environment Active application environment/profile (e.g., "dev", "prod")
 */
public record HealthResponse(
    String status,
    Instant timestamp,
    String environment
) {}
