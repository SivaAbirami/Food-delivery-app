package com.example.fooddelivery.service;

import com.example.fooddelivery.config.CacheNames;
import com.example.fooddelivery.dto.request.MenuItemRequest;
import com.example.fooddelivery.dto.request.MenuItemStatusRequest;
import com.example.fooddelivery.dto.response.MenuItemResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("Menu Caching Integration Tests")
class MenuCacheTest {

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private MenuItemRepository menuItemRepository;

    @MockBean
    private RestaurantRepository restaurantRepository;

    private User owner;
    private CustomUserDetails ownerDetails;
    private Restaurant restaurant;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        // Clear all relevant caches before each test
        for (String name : List.of(CacheNames.RESTAURANT_MENUS, CacheNames.MENU_ITEMS)) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }

        owner = User.builder()
                .id(1L)
                .name("Chef Mario")
                .email("mario@test.com")
                .role(Role.RESTAURANT_OWNER)
                .build();
        ownerDetails = new CustomUserDetails(owner);

        restaurant = Restaurant.builder()
                .id(50L)
                .name("Mario Trattoria")
                .owner(owner)
                .isOpen(true)
                .build();

        menuItem = MenuItem.builder()
                .id(500L)
                .name("Margherita Pizza")
                .description("Tomato sauce and mozzarella")
                .price(new BigDecimal("299.00"))
                .category("Pizza")
                .isAvailable(true)
                .restaurant(restaurant)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("MenuItem Detail: First call caches result, subsequent call hits cache")
    void testMenuItemDetailCacheHitAndMiss() {
        when(menuItemRepository.findById(500L)).thenReturn(Optional.of(menuItem));

        // First call - cache miss
        MenuItemResponse first = menuItemService.getMenuItemById(500L);
        assertThat(first).isNotNull();
        assertThat(first.price()).isEqualByComparingTo(new BigDecimal("299.00"));
        verify(menuItemRepository, times(1)).findById(500L);

        // Second call - cache hit
        MenuItemResponse second = menuItemService.getMenuItemById(500L);
        assertThat(second).isNotNull();
        assertThat(second.price()).isEqualByComparingTo(new BigDecimal("299.00"));
        verify(menuItemRepository, times(1)).findById(500L);
    }

    @Test
    @DisplayName("Menu List: Filter variations (category, availability) do not collide in cache")
    void testMenuListFilterSeparation() {
        Pageable pageable = PageRequest.of(0, 10);
        when(restaurantRepository.existsById(50L)).thenReturn(true);

        when(menuItemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(menuItem), pageable, 1));

        // Query category=Pizza
        PagedResponse<MenuItemResponse> pizzaRes = menuItemService.getMenuItemsByRestaurant(50L, "Pizza", null, null, pageable);
        assertThat(pizzaRes.content()).hasSize(1);
        verify(menuItemRepository, times(1)).findAll(any(Specification.class), eq(pageable));

        // Query category=Pizza again (cache hit)
        PagedResponse<MenuItemResponse> pizzaCached = menuItemService.getMenuItemsByRestaurant(50L, "Pizza", null, null, pageable);
        assertThat(pizzaCached.content()).hasSize(1);
        verify(menuItemRepository, times(1)).findAll(any(Specification.class), eq(pageable));

        // Query category=Pasta (different filter -> cache miss, calls repository)
        PagedResponse<MenuItemResponse> pastaRes = menuItemService.getMenuItemsByRestaurant(50L, "Pasta", null, null, pageable);
        verify(menuItemRepository, times(2)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Price update invalidates cache: Updated price is immediately returned on next read")
    void testPriceUpdateInvalidation() {
        when(menuItemRepository.findById(500L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        // Cache initial price
        MenuItemResponse initial = menuItemService.getMenuItemById(500L);
        assertThat(initial.price()).isEqualByComparingTo(new BigDecimal("299.00"));
        verify(menuItemRepository, times(1)).findById(500L);

        // Update price to 320.00
        MenuItemRequest updateReq = new MenuItemRequest(
                "Margherita Pizza",
                "Tomato sauce and mozzarella",
                new BigDecimal("320.00"),
                "Pizza",
                null
        );
        menuItemService.updateMenuItem(500L, updateReq, ownerDetails);

        // Next read should be a cache miss and fetch updated price
        when(menuItemRepository.findById(500L)).thenReturn(Optional.of(menuItem));
        MenuItemResponse updated = menuItemService.getMenuItemById(500L);
        assertThat(updated.price()).isEqualByComparingTo(new BigDecimal("320.00"));
        verify(menuItemRepository, atLeast(2)).findById(500L);
    }

    @Test
    @DisplayName("Toggle availability evicts menu list and item detail caches")
    void testToggleAvailabilityEviction() {
        when(menuItemRepository.findById(500L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        // Cache item
        menuItemService.getMenuItemById(500L);
        verify(menuItemRepository, times(1)).findById(500L);

        // Toggle availability
        menuItemService.toggleAvailability(500L, new MenuItemStatusRequest(false), ownerDetails);

        // Next read should query DB again
        menuItemService.getMenuItemById(500L);
        verify(menuItemRepository, atLeast(2)).findById(500L);
    }

    @Test
    @DisplayName("Delete menu item evicts item detail and restaurant menu caches")
    void testDeleteMenuItemEviction() {
        when(menuItemRepository.findById(500L)).thenReturn(Optional.of(menuItem));

        // Cache item
        menuItemService.getMenuItemById(500L);
        verify(menuItemRepository, times(1)).findById(500L);

        // Delete item
        menuItemService.deleteMenuItem(500L, ownerDetails);
        verify(menuItemRepository).delete(menuItem);

        // Next read should call repository again
        menuItemService.getMenuItemById(500L);
        verify(menuItemRepository, atLeast(2)).findById(500L);
    }
}
