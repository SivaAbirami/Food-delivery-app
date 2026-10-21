package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.CheckoutRequest;
import com.example.fooddelivery.dto.request.OrderStatusUpdateRequest;
import com.example.fooddelivery.entity.Cart;
import com.example.fooddelivery.entity.CartItem;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.OrderItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.repository.CartItemRepository;
import com.example.fooddelivery.repository.CartRepository;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

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

    private User customer1;
    private User customer2;
    private User owner1;
    private User owner2;
    private User deliveryPartner;

    private String customer1Token;
    private String customer2Token;
    private String owner1Token;
    private String owner2Token;
    private String deliveryPartnerToken;

    private Restaurant restaurant1;
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

        customer1 = userRepository.save(User.builder()
                .name("Customer One")
                .email("cust1@orders.com")
                .password(passwordEncoder.encode("password123"))
                .phone("1112223333")
                .role(Role.CUSTOMER)
                .build());
        customer1Token = jwtService.generateToken(new CustomUserDetails(customer1), customer1.getId(), customer1.getRole());

        customer2 = userRepository.save(User.builder()
                .name("Customer Two")
                .email("cust2@orders.com")
                .password(passwordEncoder.encode("password123"))
                .phone("2223334444")
                .role(Role.CUSTOMER)
                .build());
        customer2Token = jwtService.generateToken(new CustomUserDetails(customer2), customer2.getId(), customer2.getRole());

        owner1 = userRepository.save(User.builder()
                .name("Owner One")
                .email("owner1@orders.com")
                .password(passwordEncoder.encode("password123"))
                .phone("3334445555")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner1Token = jwtService.generateToken(new CustomUserDetails(owner1), owner1.getId(), owner1.getRole());

        owner2 = userRepository.save(User.builder()
                .name("Owner Two")
                .email("owner2@orders.com")
                .password(passwordEncoder.encode("password123"))
                .phone("4445556666")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner2Token = jwtService.generateToken(new CustomUserDetails(owner2), owner2.getId(), owner2.getRole());

        deliveryPartner = userRepository.save(User.builder()
                .name("Driver Dave")
                .email("driver@orders.com")
                .password(passwordEncoder.encode("password123"))
                .phone("5556667777")
                .role(Role.DELIVERY_PARTNER)
                .build());
        deliveryPartnerToken = jwtService.generateToken(new CustomUserDetails(deliveryPartner), deliveryPartner.getId(), deliveryPartner.getRole());

        restaurant1 = restaurantRepository.save(Restaurant.builder()
                .name("Bella Italia")
                .description("Authentic Italian")
                .address("100 Rome Ave")
                .phone("1112223333")
                .owner(owner1)
                .isOpen(true)
                .build());

        menuItem1 = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant1)
                .name("Wood-Fired Pizza")
                .description("Artisan pizza with fresh basil")
                .price(new BigDecimal("300.00"))
                .category("Main Course")
                .isAvailable(true)
                .build());
    }

    @Test
    @DisplayName("POST /api/orders/checkout should return 401 UNAUTHORIZED when no token is provided")
    void checkout_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        CheckoutRequest request = new CheckoutRequest("123 Street", "None", "key-1");

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /api/orders/checkout should return 403 FORBIDDEN when user has RESTAURANT_OWNER role")
    void checkout_ShouldReturnForbidden_WhenOwnerRole() throws Exception {
        CheckoutRequest request = new CheckoutRequest("123 Street", "None", "key-1");

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("POST /api/orders/checkout should succeed, compute financials, snapshot items, and clear cart")
    void checkout_ShouldSucceed_WhenValidCart() throws Exception {
        // Setup Customer 1 Cart
        Cart cart = cartRepository.save(Cart.builder()
                .customer(customer1)
                .restaurant(restaurant1)
                .build());

        CartItem cartItem = cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .menuItem(menuItem1)
                .quantity(2)
                .price(new BigDecimal("250.00")) // Cart price differs from active MenuItem price 300.00
                .build());
        cart.addItem(cartItem);
        cartRepository.save(cart);

        CheckoutRequest request = new CheckoutRequest("123 Customer Way", "Ring doorbell", "checkout-key-100");

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.orderNumber", startsWith("ORD-")))
                .andExpect(jsonPath("$.status", is("PLACED")))
                .andExpect(jsonPath("$.restaurantName", is("Bella Italia")))
                .andExpect(jsonPath("$.deliveryAddress", is("123 Customer Way")))
                .andExpect(jsonPath("$.subtotalAmount", is(600.00))) // 2 * 300.00 (Authoritative catalog price used)
                .andExpect(jsonPath("$.deliveryFee", is(40.00)))
                .andExpect(jsonPath("$.taxAmount", is(30.00))) // 5% of 600.00
                .andExpect(jsonPath("$.totalAmount", is(670.00))) // 600 + 40 + 30
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].itemName", is("Wood-Fired Pizza")))
                .andExpect(jsonPath("$.items[0].price", is(300.00)))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].subtotal", is(600.00)));

        // Verify cart is now empty
        Cart updatedCart = cartRepository.findWithDetailsByCustomerId(customer1.getId()).orElseThrow();
        assertThat(updatedCart.getItems()).isEmpty();
        assertThat(updatedCart.getRestaurant()).isNull();
    }

    @Test
    @DisplayName("GET /api/orders should return customer's order history")
    void getCustomerOrders_ShouldReturnHistory() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-TEST1")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.PLACED)
                .deliveryAddress("123 Street")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber", is("ORD-20260914-TEST1")));
    }

    @Test
    @DisplayName("GET /api/orders/{id} should return 403 FORBIDDEN when customer views another customer's order")
    void getCustomerOrderById_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-CUST1")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.PLACED)
                .deliveryAddress("123 Street")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());

        mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel should allow customer to cancel PLACED order")
    void cancelCustomerOrder_ShouldSucceed_WhenPlaced() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-CANCEL")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.PLACED)
                .deliveryAddress("123 Street")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());

        mockMvc.perform(patch("/api/orders/" + order.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("GET /api/owner/restaurants/{restaurantId}/orders should return orders for restaurant owner")
    void getRestaurantOrders_ShouldReturnOrders_WhenOwner() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-REST1")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.PLACED)
                .deliveryAddress("123 Street")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());

        mockMvc.perform(get("/api/owner/restaurants/" + restaurant1.getId() + "/orders")
                        .header("Authorization", "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber", is("ORD-20260914-REST1")));
    }

    @Test
    @DisplayName("PATCH /api/owner/orders/{id}/status should update status to ACCEPTED")
    void updateOwnerOrderStatus_ShouldSucceed_WhenValidOwner() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-OWNER1")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.PLACED)
                .deliveryAddress("123 Street")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());

        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.ACCEPTED);

        mockMvc.perform(patch("/api/owner/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }

    @Test
    @DisplayName("GET /api/delivery/orders/available should return READY_FOR_PICKUP orders without partner")
    void getAvailableOrders_ShouldReturnAvailableOrders_ForDeliveryPartner() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-AVAIL1")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryAddress("123 Street")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());

        mockMvc.perform(get("/api/delivery/orders/available")
                        .header("Authorization", "Bearer " + deliveryPartnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber", is("ORD-20260914-AVAIL1")));
    }

    @Test
    @DisplayName("PATCH /api/delivery/orders/{id}/claim should claim order and transition to OUT_FOR_DELIVERY")
    void claimAndDeliverOrder_ShouldSucceed_ForDeliveryPartner() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-CLAIM1")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryAddress("123 Street")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());

        // 1. Claim Order
        mockMvc.perform(patch("/api/delivery/orders/" + order.getId() + "/claim")
                        .header("Authorization", "Bearer " + deliveryPartnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryPartnerId", is(deliveryPartner.getId().intValue())));

        // 2. Transition to OUT_FOR_DELIVERY
        OrderStatusUpdateRequest req1 = new OrderStatusUpdateRequest(OrderStatus.OUT_FOR_DELIVERY);
        mockMvc.perform(patch("/api/delivery/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + deliveryPartnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OUT_FOR_DELIVERY")));

        // 3. Transition to DELIVERED
        OrderStatusUpdateRequest req2 = new OrderStatusUpdateRequest(OrderStatus.DELIVERED);
        mockMvc.perform(patch("/api/delivery/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + deliveryPartnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELIVERED")));
    }
}
