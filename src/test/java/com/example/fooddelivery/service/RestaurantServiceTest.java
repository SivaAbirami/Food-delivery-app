package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.RestaurantRequest;
import com.example.fooddelivery.dto.request.RestaurantStatusRequest;
import com.example.fooddelivery.dto.response.RestaurantResponse;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.RestaurantMapper;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private RestaurantMapper restaurantMapper = new RestaurantMapper();

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private User owner1;
    private User owner2;
    private User customer;
    private CustomUserDetails owner1Details;
    private CustomUserDetails owner2Details;
    private CustomUserDetails customerDetails;
    private Restaurant restaurant1;

    @BeforeEach
    void setUp() {
        owner1 = User.builder()
                .id(1L)
                .name("Mario")
                .email("mario@example.com")
                .role(Role.RESTAURANT_OWNER)
                .build();
        owner1Details = new CustomUserDetails(owner1);

        owner2 = User.builder()
                .id(2L)
                .name("Luigi")
                .email("luigi@example.com")
                .role(Role.RESTAURANT_OWNER)
                .build();
        owner2Details = new CustomUserDetails(owner2);

        customer = User.builder()
                .id(3L)
                .name("Peach")
                .email("peach@example.com")
                .role(Role.CUSTOMER)
                .build();
        customerDetails = new CustomUserDetails(customer);

        restaurant1 = Restaurant.builder()
                .id(10L)
                .name("Mario's Pizzeria")
                .description("Authentic Italian Pizza")
                .address("123 Mushroom Way")
                .phone("1112223333")
                .owner(owner1)
                .isOpen(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create restaurant when user has RESTAURANT_OWNER role")
    void createRestaurant_ShouldPersistRestaurant_WhenOwnerRole() {
        RestaurantRequest request = new RestaurantRequest(
                "Mario's Pizzeria",
                "Authentic Italian Pizza",
                "123 Mushroom Way",
                "1112223333"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner1));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant1);

        RestaurantResponse response = restaurantService.createRestaurant(request, owner1Details);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Mario's Pizzeria", response.name());
        assertEquals(1L, response.ownerId());
        assertEquals("Mario", response.ownerName());
        assertTrue(response.isOpen());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner role attempts to create restaurant")
    void createRestaurant_ShouldThrowAccessDenied_WhenCustomerRole() {
        RestaurantRequest request = new RestaurantRequest(
                "Customer Cafe",
                "Description",
                "Address",
                "1112223333"
        );

        assertThrows(AccessDeniedException.class, () -> restaurantService.createRestaurant(request, customerDetails));
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Should return restaurant profile when found by ID")
    void getRestaurantById_ShouldReturnProfile_WhenExists() {
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));

        RestaurantResponse response = restaurantService.getRestaurantById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Mario's Pizzeria", response.name());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when restaurant ID does not exist")
    void getRestaurantById_ShouldThrowNotFound_WhenMissing() {
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> restaurantService.getRestaurantById(999L));
    }

    @Test
    @DisplayName("Should successfully update restaurant when requester is the true owner")
    void updateRestaurant_ShouldUpdateFields_WhenRequesterIsOwner() {
        RestaurantRequest updateRequest = new RestaurantRequest(
                "Mario's Premium Pizzeria",
                "Updated description",
                "456 New Road",
                "9998887777"
        );

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantResponse response = restaurantService.updateRestaurant(10L, updateRequest, owner1Details);

        assertNotNull(response);
        assertEquals("Mario's Premium Pizzeria", response.name());
        assertEquals("Updated description", response.description());
        assertEquals("456 New Road", response.address());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when a different owner attempts to update restaurant")
    void updateRestaurant_ShouldThrowAccessDenied_WhenRequesterIsNotOwner() {
        RestaurantRequest updateRequest = new RestaurantRequest(
                "Hacked Pizzeria",
                "Description",
                "Address",
                "9998887777"
        );

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));

        assertThrows(AccessDeniedException.class, () -> restaurantService.updateRestaurant(10L, updateRequest, owner2Details));
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Should successfully toggle restaurant status when requester is the true owner")
    void toggleRestaurantStatus_ShouldUpdateIsOpen_WhenRequesterIsOwner() {
        RestaurantStatusRequest statusRequest = new RestaurantStatusRequest(false);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantResponse response = restaurantService.toggleRestaurantStatus(10L, statusRequest, owner1Details);

        assertNotNull(response);
        assertFalse(response.isOpen());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when another owner tries to toggle status")
    void toggleRestaurantStatus_ShouldThrowAccessDenied_WhenRequesterIsNotOwner() {
        RestaurantStatusRequest statusRequest = new RestaurantStatusRequest(false);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));

        assertThrows(AccessDeniedException.class, () -> restaurantService.toggleRestaurantStatus(10L, statusRequest, owner2Details));
    }

    @Test
    @DisplayName("Should successfully delete restaurant when requester is the true owner")
    void deleteRestaurant_ShouldDelete_WhenRequesterIsOwner() {
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));

        restaurantService.deleteRestaurant(10L, owner1Details);

        verify(restaurantRepository).delete(restaurant1);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when another owner attempts to delete restaurant")
    void deleteRestaurant_ShouldThrowAccessDenied_WhenRequesterIsNotOwner() {
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));

        assertThrows(AccessDeniedException.class, () -> restaurantService.deleteRestaurant(10L, owner2Details));
        verify(restaurantRepository, never()).delete(any(Restaurant.class));
    }
}
