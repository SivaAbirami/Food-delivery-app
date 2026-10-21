package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.LoginRequest;
import com.example.fooddelivery.dto.request.RegisterRequest;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register should create customer account and return 201 CREATED")
    void register_ShouldReturnCreated_WhenValidCustomerRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Jane Doe",
                "jane@example.com",
                "securePassword123",
                "9876543210"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register should return 409 CONFLICT when email is already registered")
    void register_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        User existingUser = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password(passwordEncoder.encode("password123"))
                .phone("9876543210")
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest(
                "Jane Duplicate",
                "JANE@example.com", // testing case insensitivity
                "securePassword123",
                "9876543210"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("USER_ALREADY_EXISTS")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 BAD_REQUEST when validation constraints fail")
    void register_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",                 // blank name
                "invalid-email",    // invalid email format
                "123",              // password too short (< 6 chars)
                ""                  // blank phone
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.phone", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 OK with Bearer token for valid credentials")
    void login_ShouldReturnOkWithToken_WhenCredentialsAreValid() throws Exception {
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password(passwordEncoder.encode("securePassword123"))
                .phone("9876543210")
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest("jane@example.com", "securePassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(3600000)))
                .andExpect(jsonPath("$.user.email", is("jane@example.com")))
                .andExpect(jsonPath("$.user.role", is("CUSTOMER")));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 UNAUTHORIZED when password is wrong")
    void login_ShouldReturnUnauthorized_WhenPasswordIsIncorrect() throws Exception {
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password(passwordEncoder.encode("securePassword123"))
                .phone("9876543210")
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest("jane@example.com", "wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("GET /api/auth/me should return 200 OK with current user when valid JWT Bearer token is provided")
    void getCurrentUser_ShouldReturnProfile_WhenTokenIsValid() throws Exception {
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password(passwordEncoder.encode("securePassword123"))
                .phone("9876543210")
                .role(Role.CUSTOMER)
                .build();
        User saved = userRepository.save(user);

        com.example.fooddelivery.security.CustomUserDetails userDetails =
                new com.example.fooddelivery.security.CustomUserDetails(saved);
        String token = jwtService.generateToken(userDetails, saved.getId(), saved.getRole());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    @Test
    @DisplayName("GET /api/auth/me should return 401 UNAUTHORIZED when no token is provided")
    void getCurrentUser_ShouldReturnUnauthorized_WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/auth/me should return 401 UNAUTHORIZED when malformed token is provided")
    void getCurrentUser_ShouldReturnUnauthorized_WhenTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }
}
