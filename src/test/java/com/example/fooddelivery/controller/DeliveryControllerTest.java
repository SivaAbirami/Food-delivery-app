package com.example.fooddelivery.controller;

import com.example.fooddelivery.entity.Order;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DeliveryController MockMvc Tests")
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    private User driver1;
    private User driver2;

    private String customer1Token;
    private String customer2Token;
    private String owner1Token;
    private String owner2Token;
    private String driver1Token;
    private String driver2Token;

    private Restaurant restaurant1;
    private Order readyOrder;

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
                .name("Alice Customer")
                .email("alice@deliv.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("1112223333")
                .role(Role.CUSTOMER)
                .build());
        customer1Token = jwtService.generateToken(new CustomUserDetails(customer1), customer1.getId(), customer1.getRole());

        customer2 = userRepository.save(User.builder()
                .name("Bob Customer")
                .email("bob@deliv.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("2223334444")
                .role(Role.CUSTOMER)
                .build());
        customer2Token = jwtService.generateToken(new CustomUserDetails(customer2), customer2.getId(), customer2.getRole());

        owner1 = userRepository.save(User.builder()
                .name("Chef Luigi")
                .email("luigi@deliv.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("3334445555")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner1Token = jwtService.generateToken(new CustomUserDetails(owner1), owner1.getId(), owner1.getRole());

        owner2 = userRepository.save(User.builder()
                .name("Chef Mario")
                .email("mario@deliv.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("4445556666")
                .role(Role.RESTAURANT_OWNER)
                .build());
        owner2Token = jwtService.generateToken(new CustomUserDetails(owner2), owner2.getId(), owner2.getRole());

        driver1 = userRepository.save(User.builder()
                .name("Driver Dan")
                .email("dan@deliv.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("5556667777")
                .role(Role.DELIVERY_PARTNER)
                .build());
        driver1Token = jwtService.generateToken(new CustomUserDetails(driver1), driver1.getId(), driver1.getRole());

        driver2 = userRepository.save(User.builder()
                .name("Driver Dave")
                .email("dave@deliv.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("6667778888")
                .role(Role.DELIVERY_PARTNER)
                .build());
        driver2Token = jwtService.generateToken(new CustomUserDetails(driver2), driver2.getId(), driver2.getRole());

        restaurant1 = restaurantRepository.save(Restaurant.builder()
                .name("Luigi Trattoria")
                .description("Italian Delights")
                .address("100 Pasta Way")
                .phone("9998887777")
                .owner(owner1)
                .isOpen(true)
                .build());

        readyOrder = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-DELIV1")
                .customer(customer1)
                .restaurant(restaurant1)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryAddress("402 Sunset Blvd")
                .specialInstructions("Leave at reception")
                .subtotalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("355.00"))
                .build());
    }

    @Test
    @DisplayName("GET /api/deliveries/available returns 401 UNAUTHORIZED when no token is provided")
    void getAvailableDeliveries_ShouldReturn401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/deliveries/available"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("GET /api/deliveries/available returns 403 FORBIDDEN for CUSTOMER and RESTAURANT_OWNER")
    void getAvailableDeliveries_ShouldReturn403_ForNonDeliveryRoles() throws Exception {
        mockMvc.perform(get("/api/deliveries/available")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        mockMvc.perform(get("/api/deliveries/available")
                        .header("Authorization", "Bearer " + owner1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("GET /api/deliveries/available returns 200 OK with available orders for DELIVERY_PARTNER")
    void getAvailableDeliveries_ShouldReturn200_ForDeliveryPartner() throws Exception {
        mockMvc.perform(get("/api/deliveries/available")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber", is("ORD-20260914-DELIV1")))
                .andExpect(jsonPath("$.content[0].restaurantName", is("Luigi Trattoria")))
                .andExpect(jsonPath("$.content[0].status", is("READY_FOR_PICKUP")));
    }

    @Test
    @DisplayName("POST /api/deliveries/{id}/claim successfully assigns order to driver")
    void claimDelivery_ShouldSucceed_ForDriver() throws Exception {
        mockMvc.perform(post("/api/deliveries/" + readyOrder.getId() + "/claim")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(readyOrder.getId().intValue())))
                .andExpect(jsonPath("$.orderNumber", is("ORD-20260914-DELIV1")));

        // Driver 2 attempting claim on same order -> 409 Conflict
        mockMvc.perform(post("/api/deliveries/" + readyOrder.getId() + "/claim")
                        .header("Authorization", "Bearer " + driver2Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("Complete Delivery Lifecycle: Claim -> GET /my -> Pickup -> Deliver")
    void fullDeliveryFlow_ShouldSucceed() throws Exception {
        // 1. Claim
        mockMvc.perform(post("/api/deliveries/" + readyOrder.getId() + "/claim")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk());

        // 2. GET /my
        mockMvc.perform(get("/api/deliveries/my")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId", is(readyOrder.getId().intValue())));

        // 3. Pickup (READY_FOR_PICKUP -> OUT_FOR_DELIVERY)
        mockMvc.perform(patch("/api/deliveries/" + readyOrder.getId() + "/pickup")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OUT_FOR_DELIVERY")))
                .andExpect(jsonPath("$.deliveryPartnerAssigned", is(true)))
                .andExpect(jsonPath("$.deliveryPartnerDisplayName", is("Driver Dan")));

        // 4. Deliver (OUT_FOR_DELIVERY -> DELIVERED)
        mockMvc.perform(patch("/api/deliveries/" + readyOrder.getId() + "/deliver")
                        .header("Authorization", "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELIVERED")));
    }

    @Test
    @DisplayName("GET /api/orders/{id}/delivery returns tracking info to customer")
    void getCustomerOrderDeliveryStatus_ShouldSucceed_ForCustomer() throws Exception {
        mockMvc.perform(get("/api/orders/" + readyOrder.getId() + "/delivery")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(readyOrder.getId().intValue())))
                .andExpect(jsonPath("$.orderNumber", is("ORD-20260914-DELIV1")))
                .andExpect(jsonPath("$.status", is("READY_FOR_PICKUP")))
                .andExpect(jsonPath("$.restaurantName", is("Luigi Trattoria")));

        // Customer 2 viewing Customer 1's delivery -> 403 Forbidden
        mockMvc.perform(get("/api/orders/" + readyOrder.getId() + "/delivery")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("GET /api/owner/orders/{id}/delivery returns tracking info to restaurant owner")
    void getOwnerOrderDeliveryStatus_ShouldSucceed_ForOwner() throws Exception {
        mockMvc.perform(get("/api/owner/orders/" + readyOrder.getId() + "/delivery")
                        .header("Authorization", "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(readyOrder.getId().intValue())))
                .andExpect(jsonPath("$.restaurantName", is("Luigi Trattoria")));

        // Owner 2 viewing Owner 1's restaurant delivery -> 403 Forbidden
        mockMvc.perform(get("/api/owner/orders/" + readyOrder.getId() + "/delivery")
                        .header("Authorization", "Bearer " + owner2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }
}
