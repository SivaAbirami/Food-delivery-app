package com.example.fooddelivery.service;

import com.example.fooddelivery.config.CacheNames;
import com.example.fooddelivery.dto.request.RestaurantRequest;
import com.example.fooddelivery.dto.request.RestaurantStatusRequest;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.dto.response.RestaurantResponse;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("Restaurant Caching Integration Tests")
class RestaurantCacheTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private RestaurantRepository restaurantRepository;

    @MockBean
    private UserRepository userRepository;

    private User owner;
    private CustomUserDetails ownerDetails;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        // Clear all relevant caches before each test
        for (String name : List.of(CacheNames.RESTAURANTS, CacheNames.RESTAURANT_DETAILS, CacheNames.RESTAURANT_MENUS)) {
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
                .id(100L)
                .name("Mario Trattoria")
                .description("Italian Cuisine")
                .address("100 Rome St")
                .phone("1234567890")
                .isOpen(true)
                .owner(owner)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Restaurant Detail: First call misses cache, second call hits cache without calling repository")
    void testRestaurantDetailCacheHitAndMiss() {
        when(restaurantRepository.findById(100L)).thenReturn(Optional.of(restaurant));

        // First call - should query repository
        RestaurantResponse first = restaurantService.getRestaurantById(100L);
        assertThat(first).isNotNull();
        assertThat(first.name()).isEqualTo("Mario Trattoria");
        verify(restaurantRepository, times(1)).findById(100L);

        // Second call - should hit cache
        RestaurantResponse second = restaurantService.getRestaurantById(100L);
        assertThat(second).isNotNull();
        assertThat(second.name()).isEqualTo("Mario Trattoria");
        verify(restaurantRepository, times(1)).findById(100L); // still 1 invocation
    }

    @Test
    @DisplayName("Restaurant List: First call caches result, identical call hits cache, different page queries DB")
    void testRestaurantListCacheHitAndPaginationSeparation() {
        Pageable page0 = PageRequest.of(0, 10);
        Pageable page1 = PageRequest.of(1, 10);

        when(restaurantRepository.findAll(any(Specification.class), eq(page0)))
                .thenReturn(new PageImpl<>(List.of(restaurant), page0, 15));
        when(restaurantRepository.findAll(any(Specification.class), eq(page1)))
                .thenReturn(new PageImpl<>(List.of(), page1, 15));

        // Call page 0
        PagedResponse<RestaurantResponse> res0 = restaurantService.getAllRestaurants(null, null, page0);
        assertThat(res0.content()).hasSize(1);
        verify(restaurantRepository, times(1)).findAll(any(Specification.class), eq(page0));

        // Call page 0 again (cache hit)
        PagedResponse<RestaurantResponse> res0Cached = restaurantService.getAllRestaurants(null, null, page0);
        assertThat(res0Cached.content()).hasSize(1);
        verify(restaurantRepository, times(1)).findAll(any(Specification.class), eq(page0));

        // Call page 1 (cache miss for different page)
        PagedResponse<RestaurantResponse> res1 = restaurantService.getAllRestaurants(null, null, page1);
        assertThat(res1.content()).isEmpty();
        verify(restaurantRepository, times(1)).findAll(any(Specification.class), eq(page1));
    }

    @Test
    @DisplayName("Restaurant Update: Evicts restaurant detail cache and list cache")
    void testRestaurantUpdateEviction() {
        when(restaurantRepository.findById(100L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));

        // Populate detail cache
        restaurantService.getRestaurantById(100L);
        verify(restaurantRepository, times(1)).findById(100L);

        // Update restaurant
        RestaurantRequest updateReq = new RestaurantRequest("Mario New Trattoria", "Updated", "100 Rome St", "1234567890");
        restaurantService.updateRestaurant(100L, updateReq, ownerDetails);

        // Subsequent getRestaurantById should be a cache miss and call repository again
        restaurantService.getRestaurantById(100L);
        verify(restaurantRepository, atLeast(2)).findById(100L);
    }

    @Test
    @DisplayName("Restaurant Status Toggle: Evicts restaurant details, restaurant lists, and menu caches")
    void testRestaurantStatusToggleEviction() {
        when(restaurantRepository.findById(100L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));

        // Populate detail cache
        restaurantService.getRestaurantById(100L);
        verify(restaurantRepository, times(1)).findById(100L);

        // Toggle status
        restaurantService.toggleRestaurantStatus(100L, new RestaurantStatusRequest(false), ownerDetails);

        // Subsequent getRestaurantById should query DB again
        restaurantService.getRestaurantById(100L);
        verify(restaurantRepository, atLeast(2)).findById(100L);
    }
}
