package com.example.fooddelivery.security;

import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "746573742d7365637265742d6b65792d666f722d666f6f642d64656c69766572792d6170702d3132333435";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);

        User user = User.builder()
                .id(100L)
                .name("Alice")
                .email("alice@example.com")
                .password("encoded_pass")
                .phone("1234567890")
                .role(Role.CUSTOMER)
                .build();
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("Should successfully generate a signed JWT token and extract subject")
    void generateToken_ShouldReturnValidToken_WhenGivenUserDetails() {
        String token = jwtService.generateToken(userDetails, 100L, Role.CUSTOMER);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("alice@example.com", jwtService.extractUsername(token));
        assertEquals(100L, jwtService.extractUserId(token));
        assertEquals("CUSTOMER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should return true for a valid token matching user details")
    void isTokenValid_ShouldReturnTrue_WhenTokenIsValidAndMatchesUser() {
        String token = jwtService.generateToken(userDetails, 100L, Role.CUSTOMER);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false when token username does not match user details")
    void isTokenValid_ShouldReturnFalse_WhenUsernameDiffers() {
        String token = jwtService.generateToken(userDetails, 100L, Role.CUSTOMER);

        User anotherUser = User.builder()
                .id(101L)
                .name("Bob")
                .email("bob@example.com")
                .role(Role.CUSTOMER)
                .build();
        CustomUserDetails anotherUserDetails = new CustomUserDetails(anotherUser);

        boolean isValid = jwtService.isTokenValid(token, anotherUserDetails);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should detect expired token correctly")
    void isTokenExpired_ShouldReturnTrue_WhenTokenHasExpired() {
        JwtService shortLivedJwtService = new JwtService(TEST_SECRET, -1000); // Already expired
        String expiredToken = shortLivedJwtService.generateToken(userDetails, 100L, Role.CUSTOMER);

        assertTrue(shortLivedJwtService.isTokenExpired(expiredToken));
        assertFalse(shortLivedJwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when secret key is too short")
    void constructor_ShouldThrowException_WhenSecretKeyIsTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short-key", EXPIRATION_MS));
    }
}
