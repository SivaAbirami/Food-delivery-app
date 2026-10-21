package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.dto.response.DeliverySummaryResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.IllegalOrderStateTransitionException;
import com.example.fooddelivery.exception.OrderConflictException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.DeliveryMapper;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.impl.DeliveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryService Unit Tests")
class DeliveryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private DeliveryMapper deliveryMapper = new DeliveryMapper();

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private User driver1;
    private User driver2;
    private User customer;
    private User otherCustomer;
    private User owner;
    private User otherOwner;

    private CustomUserDetails driver1Details;
    private CustomUserDetails driver2Details;
    private CustomUserDetails customerDetails;
    private CustomUserDetails otherCustomerDetails;
    private CustomUserDetails ownerDetails;
    private CustomUserDetails otherOwnerDetails;

    private Restaurant restaurant;
    private Order readyOrder;
    private Order claimedOrder;
    private Order outForDeliveryOrder;

    @BeforeEach
    void setUp() {
        driver1 = User.builder().id(10L).name("Driver Dan").email("dan@test.com").role(Role.DELIVERY_PARTNER).build();
        driver2 = User.builder().id(20L).name("Driver Dave").email("dave@test.com").role(Role.DELIVERY_PARTNER).build();
        customer = User.builder().id(30L).name("Alice").email("alice@test.com").role(Role.CUSTOMER).build();
        otherCustomer = User.builder().id(31L).name("Bob").email("bob@test.com").role(Role.CUSTOMER).build();
        owner = User.builder().id(40L).name("Chef Luigi").email("luigi@test.com").role(Role.RESTAURANT_OWNER).build();
        otherOwner = User.builder().id(41L).name("Chef Mario").email("mario@test.com").role(Role.RESTAURANT_OWNER).build();

        driver1Details = new CustomUserDetails(driver1);
        driver2Details = new CustomUserDetails(driver2);
        customerDetails = new CustomUserDetails(customer);
        otherCustomerDetails = new CustomUserDetails(otherCustomer);
        ownerDetails = new CustomUserDetails(owner);
        otherOwnerDetails = new CustomUserDetails(otherOwner);

        restaurant = Restaurant.builder()
                .id(100L)
                .name("Luigi Trattoria")
                .address("100 Pasta St")
                .phone("1234567890")
                .owner(owner)
                .isOpen(true)
                .build();

        readyOrder = Order.builder()
                .id(500L)
                .orderNumber("ORD-20260914-READY1")
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryAddress("402 Sunset Blvd")
                .totalAmount(new BigDecimal("500.00"))
                .deliveryPartner(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        claimedOrder = Order.builder()
                .id(501L)
                .orderNumber("ORD-20260914-CLAIM1")
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryAddress("402 Sunset Blvd")
                .totalAmount(new BigDecimal("500.00"))
                .deliveryPartner(driver1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        outForDeliveryOrder = Order.builder()
                .id(502L)
                .orderNumber("ORD-20260914-OUT1")
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .deliveryAddress("402 Sunset Blvd")
                .totalAmount(new BigDecimal("500.00"))
                .deliveryPartner(driver1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getAvailableDeliveries: Returns paginated unassigned READY_FOR_PICKUP orders")
    void testGetAvailableDeliveries() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByStatusAndDeliveryPartnerIsNull(eq(OrderStatus.READY_FOR_PICKUP), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(readyOrder), pageable, 1));

        PagedResponse<DeliverySummaryResponse> response = deliveryService.getAvailableDeliveries(pageable, driver1Details);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).orderId()).isEqualTo(500L);
        assertThat(response.content().get(0).status()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
    }

    @Test
    @DisplayName("claimDelivery: Successfully claims available order via atomic query")
    void testClaimDeliverySuccess() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(driver1));
        when(orderRepository.claimOrder(eq(500L), eq(OrderStatus.READY_FOR_PICKUP), eq(driver1), any(Instant.class)))
                .thenReturn(1);
        when(orderRepository.findWithDetailsById(500L)).thenReturn(Optional.of(claimedOrder));

        DeliverySummaryResponse response = deliveryService.claimDelivery(500L, driver1Details);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(501L);
        verify(orderRepository).claimOrder(eq(500L), eq(OrderStatus.READY_FOR_PICKUP), eq(driver1), any(Instant.class));
    }

    @Test
    @DisplayName("claimDelivery: Throws OrderConflictException when order already claimed by another driver")
    void testClaimDeliveryConflictAlreadyClaimed() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(driver2));
        when(orderRepository.claimOrder(eq(500L), eq(OrderStatus.READY_FOR_PICKUP), eq(driver2), any(Instant.class)))
                .thenReturn(0);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(claimedOrder)); // Already has driver1

        assertThrows(OrderConflictException.class, () -> deliveryService.claimDelivery(500L, driver2Details));
    }

    @Test
    @DisplayName("claimDelivery: Throws IllegalOrderStateTransitionException when order is not READY_FOR_PICKUP")
    void testClaimDeliveryWrongStatus() {
        Order preparingOrder = Order.builder()
                .id(500L)
                .status(OrderStatus.PREPARING)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(driver1));
        when(orderRepository.claimOrder(eq(500L), eq(OrderStatus.READY_FOR_PICKUP), eq(driver1), any(Instant.class)))
                .thenReturn(0);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(preparingOrder));

        assertThrows(IllegalOrderStateTransitionException.class, () -> deliveryService.claimDelivery(500L, driver1Details));
    }

    @Test
    @DisplayName("claimDelivery: Throws ResourceNotFoundException when order does not exist")
    void testClaimDeliveryNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(driver1));
        when(orderRepository.claimOrder(eq(999L), eq(OrderStatus.READY_FOR_PICKUP), eq(driver1), any(Instant.class)))
                .thenReturn(0);
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deliveryService.claimDelivery(999L, driver1Details));
    }

    @Test
    @DisplayName("getMyDeliveries: Returns deliveries assigned to authenticated driver")
    void testGetMyDeliveries() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByDeliveryPartnerId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(claimedOrder), pageable, 1));

        PagedResponse<DeliverySummaryResponse> response = deliveryService.getMyDeliveries(pageable, driver1Details);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).orderId()).isEqualTo(501L);
    }

    @Test
    @DisplayName("pickupDelivery: Assigned driver transitions order to OUT_FOR_DELIVERY")
    void testPickupDeliverySuccess() {
        when(orderRepository.findWithDetailsById(501L)).thenReturn(Optional.of(claimedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryStatusResponse response = deliveryService.pickupDelivery(501L, driver1Details);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(response.deliveryPartnerAssigned()).isTrue();
        assertThat(response.deliveryPartnerDisplayName()).isEqualTo("Driver Dan");
    }

    @Test
    @DisplayName("pickupDelivery: Throws AccessDeniedException when unassigned driver attempts pickup")
    void testPickupDeliveryWrongDriver() {
        when(orderRepository.findWithDetailsById(501L)).thenReturn(Optional.of(claimedOrder)); // assigned to driver 1

        assertThrows(AccessDeniedException.class, () -> deliveryService.pickupDelivery(501L, driver2Details));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("completeDelivery: Assigned driver transitions order to DELIVERED")
    void testCompleteDeliverySuccess() {
        when(orderRepository.findWithDetailsById(502L)).thenReturn(Optional.of(outForDeliveryOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryStatusResponse response = deliveryService.completeDelivery(502L, driver1Details);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("completeDelivery: Throws IllegalOrderStateTransitionException if order is not OUT_FOR_DELIVERY")
    void testCompleteDeliveryWrongStatus() {
        when(orderRepository.findWithDetailsById(501L)).thenReturn(Optional.of(claimedOrder)); // status is READY_FOR_PICKUP

        assertThrows(IllegalOrderStateTransitionException.class, () -> deliveryService.completeDelivery(501L, driver1Details));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("getCustomerOrderDeliveryStatus: Customer retrieves tracking for their own order")
    void testGetCustomerOrderDeliveryStatusSuccess() {
        when(orderRepository.findWithDetailsById(502L)).thenReturn(Optional.of(outForDeliveryOrder));

        DeliveryStatusResponse response = deliveryService.getCustomerOrderDeliveryStatus(502L, customerDetails);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(response.deliveryPartnerDisplayName()).isEqualTo("Driver Dan");
    }

    @Test
    @DisplayName("getCustomerOrderDeliveryStatus: Throws AccessDeniedException when customer views another customer's order")
    void testGetCustomerOrderDeliveryStatusForbidden() {
        when(orderRepository.findWithDetailsById(502L)).thenReturn(Optional.of(outForDeliveryOrder));

        assertThrows(AccessDeniedException.class, () -> deliveryService.getCustomerOrderDeliveryStatus(502L, otherCustomerDetails));
    }

    @Test
    @DisplayName("getOwnerOrderDeliveryStatus: Restaurant owner retrieves tracking for their own restaurant order")
    void testGetOwnerOrderDeliveryStatusSuccess() {
        when(orderRepository.findWithDetailsById(502L)).thenReturn(Optional.of(outForDeliveryOrder));

        DeliveryStatusResponse response = deliveryService.getOwnerOrderDeliveryStatus(502L, ownerDetails);

        assertThat(response).isNotNull();
        assertThat(response.restaurantName()).isEqualTo("Luigi Trattoria");
    }

    @Test
    @DisplayName("getOwnerOrderDeliveryStatus: Throws AccessDeniedException when owner views another restaurant's order")
    void testGetOwnerOrderDeliveryStatusForbidden() {
        when(orderRepository.findWithDetailsById(502L)).thenReturn(Optional.of(outForDeliveryOrder));

        assertThrows(AccessDeniedException.class, () -> deliveryService.getOwnerOrderDeliveryStatus(502L, otherOwnerDetails));
    }
}
