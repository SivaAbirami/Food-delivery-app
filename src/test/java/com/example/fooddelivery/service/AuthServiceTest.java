package com.example.fooddelivery.service;

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
import com.example.fooddelivery.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("encoded_pass")
                .phone("1234567890")
                .role(Role.CUSTOMER)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register customer with CUSTOMER role and encoded password")
    void register_ShouldPersistUser_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest("John Doe", "John@example.com", "password123", "1234567890");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John Doe", response.name());
        assertEquals("john@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("john@example.com", saved.getEmail());
        assertEquals(Role.CUSTOMER, saved.getRole());
        assertEquals("encoded_pass", saved.getPassword());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when registering with duplicate email")
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123", "1234567890");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate user and return AuthResponse with JWT")
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(userDetails, 1L, Role.CUSTOMER)).thenReturn("mock.jwt.token");
        when(jwtService.getExpirationTimeMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86400000L, response.expiresIn());
        assertEquals("john@example.com", response.user().email());
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when password is incorrect")
    void login_ShouldThrowException_WhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("Should return user response from authenticated principal")
    void getCurrentUser_ShouldReturnUserResponse_WhenPrincipalProvided() {
        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);

        UserResponse response = authService.getCurrentUser(userDetails);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("john@example.com", response.email());
    }
}
