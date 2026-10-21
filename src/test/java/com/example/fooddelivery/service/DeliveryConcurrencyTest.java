package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.response.DeliverySummaryResponse;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.OrderConflictException;
import com.example.fooddelivery.repository.CartItemRepository;
import com.example.fooddelivery.repository.CartRepository;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.OrderItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Delivery Concurrency Test")
class DeliveryConcurrencyTest {

    @Autowired
    private DeliveryService deliveryService;

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

    private User driverA;
    private User driverB;
    private User customer;
    private User owner;
    private Restaurant restaurant;
    private Order order;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();

        driverA = userRepository.save(User.builder()
                .name("Driver A")
                .email("driverA@test.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("1112223333")
                .role(Role.DELIVERY_PARTNER)
                .build());

        driverB = userRepository.save(User.builder()
                .name("Driver B")
                .email("driverB@test.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("2223334444")
                .role(Role.DELIVERY_PARTNER)
                .build());

        customer = userRepository.save(User.builder()
                .name("Customer Charlie")
                .email("charlie@test.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("3334445555")
                .role(Role.CUSTOMER)
                .build());

        owner = userRepository.save(User.builder()
                .name("Owner Oliver")
                .email("oliver@test.com")
                .password(passwordEncoder.encode("pass123"))
                .phone("4445556666")
                .role(Role.RESTAURANT_OWNER)
                .build());

        restaurant = restaurantRepository.save(Restaurant.builder()
                .name("Oliver's Diner")
                .description("Tasty meals")
                .address("500 Central Ave")
                .phone("5551234567")
                .owner(owner)
                .isOpen(true)
                .build());

        order = orderRepository.save(Order.builder()
                .orderNumber("ORD-20260914-CONC1")
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryAddress("999 Speedway")
                .subtotalAmount(new BigDecimal("200.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("250.00"))
                .deliveryPartner(null)
                .build());
    }

    @Test
    @DisplayName("Two delivery partners concurrently claiming the same order results in exactly 1 winner and 1 409 Conflict")
    void concurrentClaim_ShouldAllowExactlyOneWinner() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<Long> winningDriverId = new AtomicReference<>(null);

        CustomUserDetails driverADetails = new CustomUserDetails(driverA);
        CustomUserDetails driverBDetails = new CustomUserDetails(driverB);

        // Thread 1: Driver A
        executorService.submit(() -> {
            try {
                startLatch.await();
                DeliverySummaryResponse res = deliveryService.claimDelivery(order.getId(), driverADetails);
                successCount.incrementAndGet();
                winningDriverId.set(driverA.getId());
            } catch (OrderConflictException ex) {
                conflictCount.incrementAndGet();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        // Thread 2: Driver B
        executorService.submit(() -> {
            try {
                startLatch.await();
                DeliverySummaryResponse res = deliveryService.claimDelivery(order.getId(), driverBDetails);
                successCount.incrementAndGet();
                winningDriverId.set(driverB.getId());
            } catch (OrderConflictException ex) {
                conflictCount.incrementAndGet();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        // Fire both threads simultaneously
        startLatch.countDown();
        boolean finished = finishLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Verify database state: Exactly one driver was assigned
        Order orderInDb = orderRepository.findWithDetailsById(order.getId()).orElseThrow();
        assertThat(orderInDb.getDeliveryPartner()).isNotNull();
        assertThat(orderInDb.getDeliveryPartner().getId()).isEqualTo(winningDriverId.get());
    }
}
