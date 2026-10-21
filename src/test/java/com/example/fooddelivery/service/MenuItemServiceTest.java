package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.MenuItemRequest;
import com.example.fooddelivery.dto.request.MenuItemStatusRequest;
import com.example.fooddelivery.dto.response.MenuItemResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.MenuItemMapper;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.impl.MenuItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Spy
    private MenuItemMapper menuItemMapper = new MenuItemMapper();

    @InjectMocks
    private MenuItemServiceImpl menuItemService;

    private User owner1;
    private User owner2;
    private User customer;
    private CustomUserDetails owner1Details;
    private CustomUserDetails owner2Details;
    private CustomUserDetails customerDetails;
    private Restaurant restaurant1;
    private MenuItem menuItem1;

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

        menuItem1 = MenuItem.builder()
                .id(100L)
                .name("Garlic Bread")
                .description("Toasted baguette with garlic butter")
                .price(new BigDecimal("149.00"))
                .category("Starters")
                .isAvailable(true)
                .imageUrl("https://example.com/garlic-bread.jpg")
                .restaurant(restaurant1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create menu item when user is the true restaurant owner")
    void createMenuItem_ShouldPersistMenuItem_WhenTrueOwner() {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread",
                "Toasted baguette with garlic butter",
                new BigDecimal("149.00"),
                "Starters",
                "https://example.com/garlic-bread.jpg"
        );

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem1);

        MenuItemResponse response = menuItemService.createMenuItem(10L, request, owner1Details);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(10L, response.restaurantId());
        assertEquals("Mario's Pizzeria", response.restaurantName());
        assertEquals("Garlic Bread", response.name());
        assertEquals(new BigDecimal("149.00"), response.price());
        assertEquals("Starters", response.category());
        assertTrue(response.isAvailable());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when restaurant does not exist during menu item creation")
    void createMenuItem_ShouldThrowNotFound_WhenRestaurantMissing() {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread",
                "Description",
                new BigDecimal("149.00"),
                "Starters",
                null
        );

        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.createMenuItem(999L, request, owner1Details));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner role attempts to create menu item")
    void createMenuItem_ShouldThrowAccessDenied_WhenCustomerRole() {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread",
                "Description",
                new BigDecimal("149.00"),
                "Starters",
                null
        );

        assertThrows(AccessDeniedException.class, () -> menuItemService.createMenuItem(10L, request, customerDetails));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when another owner attempts to create menu item for different restaurant")
    void createMenuItem_ShouldThrowAccessDenied_WhenDifferentOwner() {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread",
                "Description",
                new BigDecimal("149.00"),
                "Starters",
                null
        );

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant1));

        assertThrows(AccessDeniedException.class, () -> menuItemService.createMenuItem(10L, request, owner2Details));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should return menu item details when found by ID")
    void getMenuItemById_ShouldReturnDetails_WhenExists() {
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));

        MenuItemResponse response = menuItemService.getMenuItemById(100L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Garlic Bread", response.name());
        assertEquals(10L, response.restaurantId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when menu item is missing")
    void getMenuItemById_ShouldThrowNotFound_WhenMissing() {
        when(menuItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> menuItemService.getMenuItemById(999L));
    }

    @Test
    @DisplayName("Should return paginated menu items when restaurant exists")
    @SuppressWarnings("unchecked")
    void getMenuItemsByRestaurant_ShouldReturnPagedResponse_WhenRestaurantExists() {
        when(restaurantRepository.existsById(10L)).thenReturn(true);
        Page<MenuItem> page = new PageImpl<>(List.of(menuItem1), PageRequest.of(0, 10), 1);
        when(menuItemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PagedResponse<MenuItemResponse> response = menuItemService.getMenuItemsByRestaurant(
                10L, "Starters", true, "garlic", PageRequest.of(0, 10)
        );

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(1, response.content().size());
        assertEquals("Garlic Bread", response.content().get(0).name());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when querying menu for non-existent restaurant")
    void getMenuItemsByRestaurant_ShouldThrowNotFound_WhenRestaurantMissing() {
        when(restaurantRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                menuItemService.getMenuItemsByRestaurant(999L, null, null, null, PageRequest.of(0, 10))
        );
    }

    @Test
    @DisplayName("Should successfully update menu item when user is true owner of parent restaurant")
    void updateMenuItem_ShouldUpdateFields_WhenTrueOwner() {
        MenuItemRequest updateRequest = new MenuItemRequest(
                "Cheesy Garlic Bread",
                "With melted mozzarella",
                new BigDecimal("199.00"),
                "Starters",
                "https://example.com/cheesy-garlic.jpg"
        );

        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuItemService.updateMenuItem(100L, updateRequest, owner1Details);

        assertNotNull(response);
        assertEquals("Cheesy Garlic Bread", response.name());
        assertEquals("With melted mozzarella", response.description());
        assertEquals(new BigDecimal("199.00"), response.price());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when another owner tries to update menu item")
    void updateMenuItem_ShouldThrowAccessDenied_WhenDifferentOwner() {
        MenuItemRequest updateRequest = new MenuItemRequest(
                "Hacked Item",
                "Description",
                new BigDecimal("99.00"),
                "Starters",
                null
        );

        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));

        assertThrows(AccessDeniedException.class, () -> menuItemService.updateMenuItem(100L, updateRequest, owner2Details));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should successfully toggle menu item availability when requester is true owner")
    void toggleAvailability_ShouldUpdateStatus_WhenTrueOwner() {
        MenuItemStatusRequest request = new MenuItemStatusRequest(false);

        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = menuItemService.toggleAvailability(100L, request, owner1Details);

        assertNotNull(response);
        assertFalse(response.isAvailable());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when another owner tries to toggle availability")
    void toggleAvailability_ShouldThrowAccessDenied_WhenDifferentOwner() {
        MenuItemStatusRequest request = new MenuItemStatusRequest(false);

        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));

        assertThrows(AccessDeniedException.class, () -> menuItemService.toggleAvailability(100L, request, owner2Details));
    }

    @Test
    @DisplayName("Should successfully delete menu item when requester is true owner")
    void deleteMenuItem_ShouldDelete_WhenTrueOwner() {
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));

        menuItemService.deleteMenuItem(100L, owner1Details);

        verify(menuItemRepository).delete(menuItem1);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when another owner attempts to delete menu item")
    void deleteMenuItem_ShouldThrowAccessDenied_WhenDifferentOwner() {
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));

        assertThrows(AccessDeniedException.class, () -> menuItemService.deleteMenuItem(100L, owner2Details));
        verify(menuItemRepository, never()).delete(any(MenuItem.class));
    }
}
