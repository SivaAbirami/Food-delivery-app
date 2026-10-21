package com.example.fooddelivery.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for customer registration.
 * <p>
 * Notice: The 'role' field is omitted by design to prevent privilege escalation.
 * Public registration always assigns {@code Role.CUSTOMER}.
 * </p>
 */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @NotBlank(message = "Phone number is required")
        @Size(min = 7, max = 20, message = "Phone number must be between 7 and 20 characters")
        String phone
) {}
