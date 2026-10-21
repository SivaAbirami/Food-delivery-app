package com.example.fooddelivery.dto.response;

import com.example.fooddelivery.enums.Role;

import java.time.Instant;

/**
 * Immutable response DTO representing user account details without exposing sensitive fields like password.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        Instant createdAt
) {}
