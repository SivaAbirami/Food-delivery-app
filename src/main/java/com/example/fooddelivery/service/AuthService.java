package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.LoginRequest;
import com.example.fooddelivery.dto.request.RegisterRequest;
import com.example.fooddelivery.dto.response.AuthResponse;
import com.example.fooddelivery.dto.response.UserResponse;
import com.example.fooddelivery.security.CustomUserDetails;

/**
 * Service handling user registration, authentication, and identity resolution.
 */
public interface AuthService {

    /**
     * Registers a new customer in the system.
     * Always assigns {@code Role.CUSTOMER} to prevent privilege escalation.
     *
     * @param request Validated registration payload
     * @return UserResponse representing the created account
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authenticates user credentials via Spring Security and issues a signed Bearer JWT token.
     *
     * @param request Validated login payload (email and password)
     * @return AuthResponse containing access token and user profile
     */
    AuthResponse login(LoginRequest request);

    /**
     * Retrieves the profile of the currently authenticated principal.
     *
     * @param currentUser Authenticated user principal extracted from SecurityContext
     * @return UserResponse profile DTO
     */
    UserResponse getCurrentUser(CustomUserDetails currentUser);
}
