package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.AddToCartRequest;
import com.example.fooddelivery.dto.request.UpdateCartItemRequest;
import com.example.fooddelivery.entity.Cart;
import com.example.fooddelivery.entity.CartItem;
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
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private com.example.fooddelivery.repository.OrderItemRepository orderItemRepository;

    @Autowired
    private com.example.fooddelivery.repository.OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

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

    private User customer1;
    private User customer2;
    private User owner;
    private String customer1Token;
    private String customer2Token;
    private String ownerToken;

    private Restaurant restaurant1;
    private Restaurant restaurant2;
    private MenuItem menuItem1;
    private MenuItem menuItem2;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();

        customer1 = userRepository.save(User.builder()
                .name("Customer One")
                .email("customer1@cart.com")
                .password(passwordEncoder.encode("password123"))
                .phone("1112223333")
                .role(Role.CUSTOMER)
                .build());
        customer1Token = jwtService.generateToken(new CustomUserDetails(customer1), customer1.getId(), customer1.getRole());

        customer2 = userRepository.save(User.builder()
                .name("Customer Two")
                .email("customer2@cart.com")
                .password(passwordEncoder.encode("password123"))
                .phone("2223334444")
                .role(Role.CUSTOMER)
                .build());
        customer2Token = jwtService.generateToken(new CustomUserDetails(customer2), customer2.getId(), customer2.getRole());

        owner = userRepository.save(User.builder()
                .name("Owner One")
                .email("owner@cart.com")
                .password(passwordEncoder.encode("password123"))
                .phone("3334445555")
                .role(Role.RESTAURANT_OWNER)
                .build());
        ownerToken = jwtService.generateToken(new CustomUserDetails(owner), owner.getId(), owner.getRole());

        restaurant1 = restaurantRepository.save(Restaurant.builder()
                .name("Mario's Pizzeria")
                .description("Wood-fired pizzas")
                .address("123 Mario St")
                .phone("1112223333")
                .owner(owner)
                .isOpen(true)
                .build());

        restaurant2 = restaurantRepository.save(Restaurant.builder()
                .name("Luigi's Burgers")
                .description("Gourmet burgers")
                .address("456 Luigi Way")
                .phone("4445556666")
                .owner(owner)
                .isOpen(true)
                .build());

        menuItem1 = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant1)
                .name("Margherita Pizza")
                .description("Classic tomato and mozzarella")
                .price(new BigDecimal("349.00"))
                .category("Main Course")
                .isAvailable(true)
                .build());

        menuItem2 = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant2)
                .name("Cheese Burger")
                .description("Gourmet beef burger")
                .price(new BigDecimal("249.00"))
                .category("Main Course")
                .isAvailable(true)
                .build());
    }

    @Test
    @DisplayName("GET /api/cart should return 401 UNAUTHORIZED when no token is provided")
    void getCart_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("GET /api/cart should return 403 FORBIDDEN when user has RESTAURANT_OWNER role")
    void getCart_ShouldReturnForbidden_WhenOwnerRole() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("GET /api/cart should return 200 OK with empty cart for a new customer")
    void getCart_ShouldReturnEmptyCart_WhenCustomerHasNoItems() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItemCount", is(0)))
                .andExpect(jsonPath("$.totalAmount", is(0)));
    }

    @Test
    @DisplayName("POST /api/cart/items should add item to cart and calculate totals")
    void addItemToCart_ShouldAddItem_WhenValid() throws Exception {
        AddToCartRequest request = new AddToCartRequest(menuItem1.getId(), 2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId", is(restaurant1.getId().intValue())))
                .andExpect(jsonPath("$.restaurantName", is("Mario's Pizzeria")))
                .andExpect(jsonPath("$.totalItemCount", is(2)))
                .andExpect(jsonPath("$.totalAmount", is(698.00)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].menuItemId", is(menuItem1.getId().intValue())))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].unitPrice", is(349.00)))
                .andExpect(jsonPath("$.items[0].subtotal", is(698.00)));
    }

    @Test
    @DisplayName("POST /api/cart/items should return 409 CONFLICT when adding item from different restaurant")
    void addItemToCart_ShouldReturnConflict_WhenAddingFromDifferentRestaurant() throws Exception {
        // Step 1: Add Item from Restaurant 1
        AddToCartRequest request1 = new AddToCartRequest(menuItem1.getId(), 1);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Step 2: Attempt to Add Item from Restaurant 2
        AddToCartRequest request2 = new AddToCartRequest(menuItem2.getId(), 1);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("CART_RESTAURANT_CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("Cart already contains items from another restaurant")));
    }

    @Test
    @DisplayName("POST /api/cart/items should return 400 BAD REQUEST when validation fails")
    void addItemToCart_ShouldReturnBadRequest_WhenInvalidPayload() throws Exception {
        AddToCartRequest invalidRequest = new AddToCartRequest(null, 0);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasKey("menuItemId")))
                .andExpect(jsonPath("$.validationErrors", hasKey("quantity")));
    }

    @Test
    @DisplayName("PUT /api/cart/items/{cartItemId} should update quantity")
    void updateCartItem_ShouldUpdateQuantity_WhenValid() throws Exception {
        Cart cart = cartRepository.save(Cart.builder()
                .customer(customer1)
                .restaurant(restaurant1)
                .build());

        CartItem cartItem = cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .menuItem(menuItem1)
                .quantity(1)
                .price(menuItem1.getPrice())
                .build());
        cart.addItem(cartItem);
        cartRepository.save(cart);

        UpdateCartItemRequest request = new UpdateCartItemRequest(4);

        mockMvc.perform(put("/api/cart/items/" + cartItem.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItemCount", is(4)))
                .andExpect(jsonPath("$.totalAmount", is(1396.00)));
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{cartItemId} should remove specific item")
    void removeCartItem_ShouldRemoveItem_WhenValid() throws Exception {
        Cart cart = cartRepository.save(Cart.builder()
                .customer(customer1)
                .restaurant(restaurant1)
                .build());

        CartItem cartItem = cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .menuItem(menuItem1)
                .quantity(1)
                .price(menuItem1.getPrice())
                .build());
        cart.addItem(cartItem);
        cartRepository.save(cart);

        mockMvc.perform(delete("/api/cart/items/" + cartItem.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItemCount", is(0)));
    }

    @Test
    @DisplayName("DELETE /api/cart should clear cart and return 204 NO CONTENT")
    void clearCart_ShouldClearAllItems() throws Exception {
        Cart cart = cartRepository.save(Cart.builder()
                .customer(customer1)
                .restaurant(restaurant1)
                .build());

        CartItem cartItem = cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .menuItem(menuItem1)
                .quantity(2)
                .price(menuItem1.getPrice())
                .build());
        cart.addItem(cartItem);
        cartRepository.save(cart);

        mockMvc.perform(delete("/api/cart")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isNoContent());

        // Verify cart is cleared
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItemCount", is(0)));
    }
}
