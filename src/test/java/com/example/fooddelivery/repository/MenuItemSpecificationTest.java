package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.repository.CartItemRepository;
import com.example.fooddelivery.repository.CartRepository;
import com.example.fooddelivery.repository.specification.MenuItemSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MenuItemSpecificationTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    private Restaurant restaurant1;
    private Restaurant restaurant2;

    private MenuItem r1GarlicBread;
    private MenuItem r1GarlicSoup;
    private MenuItem r1Burger;
    private MenuItem r1SaladNullDesc;
    private MenuItem r2GarlicNaan;
    private MenuItem r2GarlicBurger;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();

        User owner1 = userRepository.save(User.builder()
                .name("Owner One")
                .email("owner1@spec.com")
                .password("hash")
                .phone("1112223333")
                .role(Role.RESTAURANT_OWNER)
                .build());

        User owner2 = userRepository.save(User.builder()
                .name("Owner Two")
                .email("owner2@spec.com")
                .password("hash")
                .phone("2223334444")
                .role(Role.RESTAURANT_OWNER)
                .build());

        restaurant1 = restaurantRepository.save(Restaurant.builder()
                .name("Italian Bistro")
                .description("Authentic Italian")
                .address("100 Rome Way")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(true)
                .build());

        restaurant2 = restaurantRepository.save(Restaurant.builder()
                .name("American Diner")
                .description("Burgers & Grills")
                .address("200 Broadway")
                .phone("2222222222")
                .owner(owner2)
                .isOpen(true)
                .build());

        // Restaurant 1 items
        r1GarlicBread = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant1)
                .name("Garlic Bread")
                .description("Toasted baguette with garlic butter")
                .price(new BigDecimal("149.00"))
                .category("Starters")
                .isAvailable(true)
                .build());

        r1GarlicSoup = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant1)
                .name("Garlic Soup")
                .description("Warm soup")
                .price(new BigDecimal("99.00"))
                .category("Starters")
                .isAvailable(false) // Unavailable
                .build());

        r1Burger = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant1)
                .name("Cheese Burger")
                .description("Beef patty with cheddar")
                .price(new BigDecimal("299.00"))
                .category("Main Course")
                .isAvailable(true)
                .build());

        r1SaladNullDesc = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant1)
                .name("Green Salad")
                .description(null) // Null description
                .price(new BigDecimal("120.00"))
                .category("Starters")
                .isAvailable(true)
                .build());

        // Restaurant 2 items
        r2GarlicNaan = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant2)
                .name("Garlic Naan")
                .description("Tandoori bread with garlic")
                .price(new BigDecimal("80.00"))
                .category("Starters")
                .isAvailable(true)
                .build());

        r2GarlicBurger = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant2)
                .name("Garlic Burger")
                .description("Loaded with roasted garlic")
                .price(new BigDecimal("350.00"))
                .category("Main Course")
                .isAvailable(true)
                .build());
    }

    @Test
    @DisplayName("Specification should strictly group: restaurantId AND category AND available AND (name OR description search)")
    void withFilters_ShouldStrictlyGroupAllFilters() {
        Specification<MenuItem> spec = MenuItemSpecification.withFilters(
                restaurant1.getId(), "Starters", true, "garlic"
        );

        Page<MenuItem> result = menuItemRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Garlic Bread", result.getContent().get(0).getName());
        assertEquals(r1GarlicBread.getId(), result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Specification must NEVER leak menu items from another restaurant when searching by keyword")
    void withFilters_ShouldNeverLeakItemsFromOtherRestaurants() {
        Specification<MenuItem> spec = MenuItemSpecification.withFilters(
                restaurant1.getId(), null, null, "garlic"
        );

        Page<MenuItem> result = menuItemRepository.findAll(spec, PageRequest.of(0, 10));

        List<String> names = result.getContent().stream().map(MenuItem::getName).toList();
        assertEquals(2, result.getTotalElements());
        assertTrue(names.contains("Garlic Bread"));
        assertTrue(names.contains("Garlic Soup"));
        assertFalse(names.contains("Garlic Naan"), "Must not contain Restaurant 2's Garlic Naan");
        assertFalse(names.contains("Garlic Burger"), "Must not contain Restaurant 2's Garlic Burger");
    }

    @Test
    @DisplayName("Specification should filter by category case-insensitively")
    void withFilters_ShouldFilterByCategoryCaseInsensitive() {
        Specification<MenuItem> spec = MenuItemSpecification.withFilters(
                restaurant1.getId(), "sTaRtErS", null, null
        );

        Page<MenuItem> result = menuItemRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(3, result.getTotalElements());
        List<String> names = result.getContent().stream().map(MenuItem::getName).toList();
        assertTrue(names.contains("Garlic Bread"));
        assertTrue(names.contains("Garlic Soup"));
        assertTrue(names.contains("Green Salad"));
        assertFalse(names.contains("Cheese Burger"));
    }

    @Test
    @DisplayName("Specification should filter by availability flag")
    void withFilters_ShouldFilterByAvailability() {
        Specification<MenuItem> availableSpec = MenuItemSpecification.withFilters(
                restaurant1.getId(), null, true, null
        );
        Page<MenuItem> availableResult = menuItemRepository.findAll(availableSpec, PageRequest.of(0, 10));
        assertEquals(3, availableResult.getTotalElements());

        Specification<MenuItem> unavailableSpec = MenuItemSpecification.withFilters(
                restaurant1.getId(), null, false, null
        );
        Page<MenuItem> unavailableResult = menuItemRepository.findAll(unavailableSpec, PageRequest.of(0, 10));
        assertEquals(1, unavailableResult.getTotalElements());
        assertEquals("Garlic Soup", unavailableResult.getContent().get(0).getName());
    }

    @Test
    @DisplayName("Specification should safely handle null descriptions during text search")
    void withFilters_ShouldHandleNullDescriptionSafely() {
        Specification<MenuItem> spec = MenuItemSpecification.withFilters(
                restaurant1.getId(), null, null, "salad"
        );

        Page<MenuItem> result = menuItemRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Green Salad", result.getContent().get(0).getName());
    }
}
