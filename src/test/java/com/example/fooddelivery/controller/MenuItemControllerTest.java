package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.MenuItemRequest;
import com.example.fooddelivery.dto.request.MenuItemStatusRequest;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.repository.CartItemRepository;
import com.example.fooddelivery.repository.CartRepository;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.fooddelivery.repository.OrderItemRepository orderItemRepository;

    @Autowired
    private com.example.fooddelivery.repository.OrderRepository orderRepository;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner1;
    private User owner2;
    private User customer;
    private String owner1Token;
    private String owner2Token;
    private String customerToken;

    private Restaurant restaurant1;
    private Restaurant restaurant2;
    private MenuItem menuItem1;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();

        owner1 = userRepository.save(User.builder()
                .name("Owner One")
                .email("owner1@menu.com")
                .password(passwordEncoder.encode("password123"))
                .phone("1112223333")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner1Token = jwtService.generateToken(new CustomUserDetails(owner1), owner1.getId(), owner1.getRole());

        owner2 = userRepository.save(User.builder()
                .name("Owner Two")
                .email("owner2@menu.com")
                .password(passwordEncoder.encode("password123"))
                .phone("2223334444")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner2Token = jwtService.generateToken(new CustomUserDetails(owner2), owner2.getId(), owner2.getRole());

        customer = userRepository.save(User.builder()
                .name("Customer User")
                .email("customer@menu.com")
                .password(passwordEncoder.encode("password123"))
                .phone("3334445555")
                .role(Role.CUSTOMER)
                .build());
        customerToken = jwtService.generateToken(new CustomUserDetails(customer), customer.getId(), customer.getRole());

        restaurant1 = restaurantRepository.save(Restaurant.builder()
                .name("Mario's Pizzeria")
                .description("Wood-fired pizzas")
                .address("123 Mario St")
                .phone("1112223333")
                .owner(owner1)
                .isOpen(true)
                .build());

        restaurant2 = restaurantRepository.save(Restaurant.builder()
                .name("Luigi's Burgers")
                .description("Gourmet burgers")
                .address("456 Luigi Way")
                .phone("4445556666")
                .owner(owner2)
                .isOpen(true)
                .build());

        menuItem1 = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant1)
                .name("Margherita Pizza")
                .description("Classic tomato and mozzarella")
                .price(new BigDecimal("349.00"))
                .category("Main Course")
                .isAvailable(true)
                .imageUrl("https://example.com/margherita.jpg")
                .build());
    }

    @Test
    @DisplayName("GET /api/restaurants/{id}/menu should be publicly accessible without authentication")
    void getRestaurantMenu_ShouldReturnOk_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/restaurants/" + restaurant1.getId() + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Margherita Pizza")))
                .andExpect(jsonPath("$.content[0].restaurantId", is(restaurant1.getId().intValue())))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id}/menu should return 404 NOT FOUND for non-existent restaurant")
    void getRestaurantMenu_ShouldReturnNotFound_WhenRestaurantMissing() throws Exception {
        mockMvc.perform(get("/api/restaurants/999999/menu"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Restaurant not found with id: 999999")));
    }

    @Test
    @DisplayName("GET /api/menu-items/{id} should be publicly accessible without authentication")
    void getMenuItemById_ShouldReturnOk_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/menu-items/" + menuItem1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(menuItem1.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Margherita Pizza")))
                .andExpect(jsonPath("$.price", is(349.00)))
                .andExpect(jsonPath("$.restaurantName", is("Mario's Pizzeria")));
    }

    @Test
    @DisplayName("GET /api/menu-items/{id} should return 404 NOT FOUND when item does not exist")
    void getMenuItemById_ShouldReturnNotFound_WhenMissing() throws Exception {
        mockMvc.perform(get("/api/menu-items/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("POST /api/restaurants/{id}/menu should return 401 UNAUTHORIZED when no token is provided")
    void createMenuItem_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread", "Warm garlic bread", new BigDecimal("149.00"), "Starters", null
        );

        mockMvc.perform(post("/api/restaurants/" + restaurant1.getId() + "/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /api/restaurants/{id}/menu should return 403 FORBIDDEN when user has CUSTOMER role")
    void createMenuItem_ShouldReturnForbidden_WhenCustomerRole() throws Exception {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread", "Warm garlic bread", new BigDecimal("149.00"), "Starters", null
        );

        mockMvc.perform(post("/api/restaurants/" + restaurant1.getId() + "/menu")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("POST /api/restaurants/{id}/menu should return 403 FORBIDDEN when user is owner of a different restaurant")
    void createMenuItem_ShouldReturnForbidden_WhenDifferentOwner() throws Exception {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread", "Warm garlic bread", new BigDecimal("149.00"), "Starters", null
        );

        // Owner 2 tries to create menu item on Restaurant 1 (owned by Owner 1)
        mockMvc.perform(post("/api/restaurants/" + restaurant1.getId() + "/menu")
                        .header("Authorization", "Bearer " + owner2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("POST /api/restaurants/{id}/menu should return 201 CREATED when authenticated user owns restaurant")
    void createMenuItem_ShouldReturnCreated_WhenTrueOwner() throws Exception {
        MenuItemRequest request = new MenuItemRequest(
                "Garlic Bread", "Warm garlic bread", new BigDecimal("149.00"), "Starters", "https://example.com/gb.jpg"
        );

        mockMvc.perform(post("/api/restaurants/" + restaurant1.getId() + "/menu")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Garlic Bread")))
                .andExpect(jsonPath("$.price", is(149.00)))
                .andExpect(jsonPath("$.category", is("Starters")))
                .andExpect(jsonPath("$.isAvailable", is(true)))
                .andExpect(jsonPath("$.restaurantId", is(restaurant1.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/restaurants/{id}/menu should return 400 BAD REQUEST when validation constraints fail")
    void createMenuItem_ShouldReturnBadRequest_WhenInvalidPayload() throws Exception {
        MenuItemRequest invalidRequest = new MenuItemRequest(
                "", // Blank name
                "Description",
                new BigDecimal("-5.00"), // Negative price
                "", // Blank category
                null
        );

        mockMvc.perform(post("/api/restaurants/" + restaurant1.getId() + "/menu")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasKey("name")))
                .andExpect(jsonPath("$.validationErrors", hasKey("price")))
                .andExpect(jsonPath("$.validationErrors", hasKey("category")));
    }

    @Test
    @DisplayName("PUT /api/menu-items/{id} should return 200 OK when updated by true owner of parent restaurant")
    void updateMenuItem_ShouldReturnOk_WhenTrueOwner() throws Exception {
        MenuItemRequest updateRequest = new MenuItemRequest(
                "Super Margherita",
                "Extra fresh mozzarella & basil",
                new BigDecimal("399.00"),
                "Main Course",
                "https://example.com/super-margherita.jpg"
        );

        mockMvc.perform(put("/api/menu-items/" + menuItem1.getId())
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Super Margherita")))
                .andExpect(jsonPath("$.price", is(399.00)));
    }

    @Test
    @DisplayName("PUT /api/menu-items/{id} should return 403 FORBIDDEN when updated by a different owner")
    void updateMenuItem_ShouldReturnForbidden_WhenDifferentOwner() throws Exception {
        MenuItemRequest updateRequest = new MenuItemRequest(
                "Hacked Pizza", "Illegal description", new BigDecimal("10.00"), "Main Course", null
        );

        // Owner 2 tries to update Owner 1's menu item
        mockMvc.perform(put("/api/menu-items/" + menuItem1.getId())
                        .header("Authorization", "Bearer " + owner2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("PATCH /api/menu-items/{id}/status should return 200 OK when toggled by true owner")
    void toggleAvailability_ShouldReturnOk_WhenTrueOwner() throws Exception {
        MenuItemStatusRequest request = new MenuItemStatusRequest(false);

        mockMvc.perform(patch("/api/menu-items/" + menuItem1.getId() + "/status")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAvailable", is(false)));
    }

    @Test
    @DisplayName("PATCH /api/menu-items/{id}/status should return 403 FORBIDDEN when toggled by another owner")
    void toggleAvailability_ShouldReturnForbidden_WhenDifferentOwner() throws Exception {
        MenuItemStatusRequest request = new MenuItemStatusRequest(false);

        mockMvc.perform(patch("/api/menu-items/" + menuItem1.getId() + "/status")
                        .header("Authorization", "Bearer " + owner2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("DELETE /api/menu-items/{id} should return 204 NO CONTENT when deleted by true owner")
    void deleteMenuItem_ShouldReturnNoContent_WhenTrueOwner() throws Exception {
        mockMvc.perform(delete("/api/menu-items/" + menuItem1.getId())
                        .header("Authorization", "Bearer " + owner1Token))
                .andExpect(status().isNoContent());

        // Verify item is actually deleted
        mockMvc.perform(get("/api/menu-items/" + menuItem1.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/menu-items/{id} should return 403 FORBIDDEN when deleted by another owner")
    void deleteMenuItem_ShouldReturnForbidden_WhenDifferentOwner() throws Exception {
        mockMvc.perform(delete("/api/menu-items/" + menuItem1.getId())
                        .header("Authorization", "Bearer " + owner2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("Pagination should clamp page size to maximum limit (50)")
    void getRestaurantMenu_ShouldClampMaxPageSize() throws Exception {
        mockMvc.perform(get("/api/restaurants/" + restaurant1.getId() + "/menu?page=0&size=1000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize", is(50)))
                .andExpect(jsonPath("$.pageNumber", is(0)));
    }
}
