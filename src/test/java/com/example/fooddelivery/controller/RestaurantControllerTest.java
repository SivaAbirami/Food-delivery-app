package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.RestaurantRequest;
import com.example.fooddelivery.dto.request.RestaurantStatusRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestaurantControllerTest {

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
                .email("owner1@example.com")
                .password(passwordEncoder.encode("password123"))
                .phone("1112223333")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner1Token = jwtService.generateToken(new CustomUserDetails(owner1), owner1.getId(), owner1.getRole());

        owner2 = userRepository.save(User.builder()
                .name("Owner Two")
                .email("owner2@example.com")
                .password(passwordEncoder.encode("password123"))
                .phone("2223334444")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner2Token = jwtService.generateToken(new CustomUserDetails(owner2), owner2.getId(), owner2.getRole());

        customer = userRepository.save(User.builder()
                .name("Customer User")
                .email("customer@example.com")
                .password(passwordEncoder.encode("password123"))
                .phone("3334445555")
                .role(Role.CUSTOMER)
                .build());
        customerToken = jwtService.generateToken(new CustomUserDetails(customer), customer.getId(), customer.getRole());
    }

    @Test
    @DisplayName("GET /api/restaurants should be publicly accessible without authentication")
    void getAllRestaurants_ShouldReturnOk_WhenUnauthenticated() throws Exception {
        restaurantRepository.save(Restaurant.builder()
                .name("Luigi's Trattoria")
                .description("Italian Pasta")
                .address("100 Pasta Lane")
                .phone("1234567890")
                .owner(owner1)
                .isOpen(true)
                .build());

        mockMvc.perform(get("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Luigi's Trattoria")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.pageNumber", is(0)));
    }

    @Test
    @DisplayName("GET /api/restaurants search and isOpen boolean precedence test")
    void searchRestaurants_ShouldEnforceStrictBooleanGrouping() throws Exception {
        // Restaurant A: name contains pizza, but isOpen = false
        restaurantRepository.save(Restaurant.builder()
                .name("Pizza Palace")
                .description("Italian traditional kitchen")
                .address("101 Pizza Road")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(false)
                .build());

        // Restaurant B: description contains pizza, and isOpen = true
        restaurantRepository.save(Restaurant.builder()
                .name("Burger House")
                .description("Handcrafted pizza and burgers")
                .address("102 Burger Ave")
                .phone("2222222222")
                .owner(owner1)
                .isOpen(true)
                .build());

        // Restaurant C: unrelated food, isOpen = true
        restaurantRepository.save(Restaurant.builder()
                .name("Sushi World")
                .description("Fresh sashimi and rolls")
                .address("103 Ocean Way")
                .phone("3333333333")
                .owner(owner2)
                .isOpen(true)
                .build());

        // Search: search=pizza & isOpen=true
        // Must return ONLY Restaurant B
        mockMvc.perform(get("/api/restaurants")
                        .param("search", "pizza")
                        .param("isOpen", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Burger House")));
    }

    @Test
    @DisplayName("GET /api/restaurants pagination parameters test")
    void getAllRestaurants_ShouldPaginateCorrectly() throws Exception {
        for (int i = 1; i <= 3; i++) {
            restaurantRepository.save(Restaurant.builder()
                    .name("Cafe " + i)
                    .description("Coffee shop " + i)
                    .address("Address " + i)
                    .phone("100000000" + i)
                    .owner(owner1)
                    .isOpen(true)
                    .build());
        }

        mockMvc.perform(get("/api/restaurants")
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.lastPage", is(false)));
    }

    @Test
    @DisplayName("POST /api/restaurants should allow RESTAURANT_OWNER to create a restaurant (201)")
    void createRestaurant_ShouldReturnCreated_WhenOwner() throws Exception {
        RestaurantRequest request = new RestaurantRequest(
                "Taco Fiesta",
                "Authentic Mexican Tacos",
                "500 Sunset Blvd",
                "9876543210"
        );

        mockMvc.perform(post("/api/restaurants")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Taco Fiesta")))
                .andExpect(jsonPath("$.ownerId", is(owner1.getId().intValue())))
                .andExpect(jsonPath("$.ownerName", is("Owner One")))
                .andExpect(jsonPath("$.isOpen", is(true)));
    }

    @Test
    @DisplayName("POST /api/restaurants should return 403 FORBIDDEN when called by CUSTOMER")
    void createRestaurant_ShouldReturnForbidden_WhenCustomer() throws Exception {
        RestaurantRequest request = new RestaurantRequest(
                "Customer Cafe",
                "Description",
                "Address",
                "9876543210"
        );

        mockMvc.perform(post("/api/restaurants")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("POST /api/restaurants should return 401 UNAUTHORIZED when unauthenticated")
    void createRestaurant_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        RestaurantRequest request = new RestaurantRequest(
                "Anonymous Cafe",
                "Description",
                "Address",
                "9876543210"
        );

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/restaurants/my should return only restaurants belonging to the authenticated owner")
    void getMyRestaurants_ShouldReturnOnlyOwnedRestaurants() throws Exception {
        restaurantRepository.save(Restaurant.builder()
                .name("Owner 1 Outlet")
                .address("Road 1")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(true)
                .build());

        restaurantRepository.save(Restaurant.builder()
                .name("Owner 2 Outlet")
                .address("Road 2")
                .phone("2222222222")
                .owner(owner2)
                .isOpen(true)
                .build());

        mockMvc.perform(get("/api/restaurants/my")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Owner 1 Outlet")));
    }

    @Test
    @DisplayName("PUT /api/restaurants/{id} should succeed when called by the true owner (200)")
    void updateRestaurant_ShouldReturnOk_WhenTrueOwner() throws Exception {
        Restaurant restaurant = restaurantRepository.save(Restaurant.builder()
                .name("Old Name")
                .description("Old Desc")
                .address("Old Address")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(true)
                .build());

        RestaurantRequest updateRequest = new RestaurantRequest(
                "New Name",
                "New Desc",
                "New Address",
                "9999999999"
        );

        mockMvc.perform(put("/api/restaurants/" + restaurant.getId())
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.address", is("New Address")));
    }

    @Test
    @DisplayName("PUT /api/restaurants/{id} should return 403 FORBIDDEN when called by a different owner")
    void updateRestaurant_ShouldReturnForbidden_WhenDifferentOwner() throws Exception {
        Restaurant restaurant = restaurantRepository.save(Restaurant.builder()
                .name("Owner 1 Restaurant")
                .address("Road 1")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(true)
                .build());

        RestaurantRequest updateRequest = new RestaurantRequest(
                "Hacked Name",
                "Hacked Desc",
                "Hacked Address",
                "9999999999"
        );

        mockMvc.perform(put("/api/restaurants/" + restaurant.getId())
                        .header("Authorization", "Bearer " + owner2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("PATCH /api/restaurants/{id}/status should toggle isOpen status by owner (200)")
    void toggleRestaurantStatus_ShouldUpdateStatus_WhenOwner() throws Exception {
        Restaurant restaurant = restaurantRepository.save(Restaurant.builder()
                .name("Bakery Outlet")
                .address("Bakery Lane")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(true)
                .build());

        RestaurantStatusRequest statusRequest = new RestaurantStatusRequest(false);

        mockMvc.perform(patch("/api/restaurants/" + restaurant.getId() + "/status")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOpen", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/restaurants/{id} should delete restaurant when called by owner (204)")
    void deleteRestaurant_ShouldReturnNoContent_WhenOwner() throws Exception {
        Restaurant restaurant = restaurantRepository.save(Restaurant.builder()
                .name("To Be Deleted")
                .address("Lane 1")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(true)
                .build());

        mockMvc.perform(delete("/api/restaurants/" + restaurant.getId())
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/" + restaurant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/restaurants/{id} should return 403 FORBIDDEN when called by different owner")
    void deleteRestaurant_ShouldReturnForbidden_WhenDifferentOwner() throws Exception {
        Restaurant restaurant = restaurantRepository.save(Restaurant.builder()
                .name("Owner 1 Restaurant")
                .address("Lane 1")
                .phone("1111111111")
                .owner(owner1)
                .isOpen(true)
                .build());

        mockMvc.perform(delete("/api/restaurants/" + restaurant.getId())
                        .header("Authorization", "Bearer " + owner2Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
