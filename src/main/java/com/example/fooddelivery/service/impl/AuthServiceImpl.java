package com.example.fooddelivery.service.impl;

import com.example.fooddelivery.dto.request.LoginRequest;
import com.example.fooddelivery.dto.request.RegisterRequest;
import com.example.fooddelivery.dto.response.AuthResponse;
import com.example.fooddelivery.dto.response.UserResponse;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.UserAlreadyExistsException;
import com.example.fooddelivery.mapper.UserMapper;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.security.JwtService;
import com.example.fooddelivery.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthService business logic.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // 1. Check if email already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email '" + normalizedEmail + "' already exists");
        }

        // 2. Build User entity — always assigning Role.CUSTOMER for public registration
        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone().trim())
                .role(Role.CUSTOMER)
                .build();

        try {
            User savedUser = userRepository.save(user);
            return userMapper.toResponse(savedUser);
        } catch (DataIntegrityViolationException e) {
            // Handle race condition where another concurrent request registered the same email
            throw new UserAlreadyExistsException("User with email '" + normalizedEmail + "' already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // Delegate credential verification to Spring Security's AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Generate signed JWT token
        String token = jwtService.generateToken(userDetails, user.getId(), user.getRole());

        return AuthResponse.of(
                token,
                jwtService.getExpirationTimeMs(),
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new IllegalArgumentException("Authenticated user principal is required");
        }
        return userMapper.toResponse(currentUser.getUser());
    }
}
