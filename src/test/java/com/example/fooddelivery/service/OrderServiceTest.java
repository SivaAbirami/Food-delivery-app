package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.CheckoutRequest;
import com.example.fooddelivery.dto.request.OrderStatusUpdateRequest;
import com.example.fooddelivery.dto.response.OrderResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.entity.Cart;
import com.example.fooddelivery.entity.CartItem;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.OrderItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.IllegalOrderStateTransitionException;
import com.example.fooddelivery.exception.OrderConflictException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.OrderMapper;
import com.example.fooddelivery.repository.CartRepository;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    private User customer;
    private User owner;
    private User deliveryPartner;
    private CustomUserDetails customerDetails;
    private CustomUserDetails ownerDetails;
    private CustomUserDetails partnerDetails;

    private Restaurant restaurant;
    private MenuItem menuItem1;
    private MenuItem menuItem2;
    private Cart cart;
    private CartItem cartItem1;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .name("Customer One")
                .email("cust@test.com")
                .role(Role.CUSTOMER)
                .build();
        customerDetails = new CustomUserDetails(customer);

        owner = User.builder()
                .id(2L)
                .name("Owner One")
                .email("owner@test.com")
                .role(Role.RESTAURANT_OWNER)
                .build();
        ownerDetails = new CustomUserDetails(owner);

        deliveryPartner = User.builder()
                .id(3L)
                .name("Delivery Partner")
                .email("driver@test.com")
                .role(Role.DELIVERY_PARTNER)
                .build();
        partnerDetails = new CustomUserDetails(deliveryPartner);

        restaurant = Restaurant.builder()
                .id(10L)
                .name("Luigi Italian Diner")
                .isOpen(true)
                .owner(owner)
                .build();

        menuItem1 = MenuItem.builder()
                .id(101L)
                .restaurant(restaurant)
                .name("Truffle Pasta")
                .price(new BigDecimal("450.00"))
                .isAvailable(true)
                .build();

        menuItem2 = MenuItem.builder()
                .id(102L)
                .restaurant(restaurant)
                .name("Garlic Bread")
                .price(new BigDecimal("150.00"))
                .isAvailable(true)
                .build();

        cartItem1 = CartItem.builder()
                .id(501L)
                .menuItem(menuItem1)
                .price(new BigDecimal("400.00")) // Cart price snapshot differs from updated menu item price
                .quantity(2)
                .build();

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem1);

        cart = Cart.builder()
                .id(201L)
                .customer(customer)
                .restaurant(restaurant)
                .items(cartItems)
                .build();
        cartItem1.setCart(cart);
    }

    @Test
    @DisplayName("Checkout should succeed with authoritative price snapshot, financials calculation, and cart clearing")
    void checkout_ShouldSucceed_WhenCartIsValid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(menuItemRepository.findById(101L)).thenReturn(Optional.of(menuItem1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            o.setCreatedAt(Instant.now());
            o.setUpdatedAt(Instant.now());
            return o;
        });

        CheckoutRequest request = new CheckoutRequest("123 Food Street", "Ring buzzer 4", "key-123");
        OrderResponse response = orderService.checkout(request, customerDetails);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(999L);
        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.restaurantId()).isEqualTo(10L);
        assertThat(response.restaurantName()).isEqualTo("Luigi Italian Diner");
        assertThat(response.deliveryAddress()).isEqualTo("123 Food Street");

        // Subtotal: 2 * 450.00 = 900.00 (Authoritative live MenuItem price used, not Cart price 400.00)
        assertThat(response.subtotalAmount()).isEqualByComparingTo("900.00");
        assertThat(response.deliveryFee()).isEqualByComparingTo("40.00");
        // Tax: 5% of 900.00 = 45.00
        assertThat(response.taxAmount()).isEqualByComparingTo("45.00");
        // Total: 900.00 + 40.00 + 45.00 = 985.00
        assertThat(response.totalAmount()).isEqualByComparingTo("985.00");

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).itemName()).isEqualTo("Truffle Pasta");
        assertThat(response.items().get(0).price()).isEqualByComparingTo("450.00");
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("900.00");

        // Verify cart is cleared
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getRestaurant()).isNull();
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("Checkout should return existing order when idempotent key matches")
    void checkout_ShouldReturnExistingOrder_WhenIdempotentKeyExists() {
        Order existingOrder = Order.builder()
                .id(888L)
                .orderNumber("ORD-20260914-EXIST1")
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PLACED)
                .subtotalAmount(new BigDecimal("900.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("45.00"))
                .totalAmount(new BigDecimal("985.00"))
                .idempotencyKey("key-123")
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existingOrder));

        CheckoutRequest request = new CheckoutRequest("123 Food Street", "Ring buzzer 4", "key-123");
        OrderResponse response = orderService.checkout(request, customerDetails);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(888L);
        assertThat(response.orderNumber()).isEqualTo("ORD-20260914-EXIST1");
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Checkout should throw exception when cart is empty")
    void checkout_ShouldThrowException_WhenCartIsEmpty() {
        cart.clearItems();
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.of(cart));

        CheckoutRequest request = new CheckoutRequest("123 Food Street", null, null);

        assertThatThrownBy(() -> orderService.checkout(request, customerDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shopping cart is empty");
    }

    @Test
    @DisplayName("Checkout should throw exception when restaurant is closed")
    void checkout_ShouldThrowException_WhenRestaurantIsClosed() {
        restaurant.setOpen(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.of(cart));

        CheckoutRequest request = new CheckoutRequest("123 Food Street", null, null);

        assertThatThrownBy(() -> orderService.checkout(request, customerDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently closed");
    }

    @Test
    @DisplayName("Checkout should throw exception when menu item is unavailable")
    void checkout_ShouldThrowException_WhenMenuItemIsUnavailable() {
        menuItem1.setAvailable(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(menuItemRepository.findById(101L)).thenReturn(Optional.of(menuItem1));

        CheckoutRequest request = new CheckoutRequest("123 Food Street", null, null);

        assertThatThrownBy(() -> orderService.checkout(request, customerDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently unavailable");
    }

    @Test
    @DisplayName("Customer should be able to cancel PLACED order")
    void cancelCustomerOrder_ShouldSucceed_WhenStatusIsPlaced() {
        Order order = Order.builder()
                .id(777L)
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PLACED)
                .subtotalAmount(new BigDecimal("450.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("22.50"))
                .totalAmount(new BigDecimal("512.50"))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findWithDetailsById(777L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelCustomerOrder(777L, customerDetails);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Customer cancellation should fail when order is already ACCEPTED")
    void cancelCustomerOrder_ShouldFail_WhenStatusIsAccepted() {
        Order order = Order.builder()
                .id(777L)
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.ACCEPTED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findWithDetailsById(777L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelCustomerOrder(777L, customerDetails))
                .isInstanceOf(IllegalOrderStateTransitionException.class)
                .hasMessageContaining("Customer can only cancel orders in 'PLACED' status");
    }

    @Test
    @DisplayName("Restaurant Owner should transition order PLACED -> ACCEPTED -> PREPARING -> READY_FOR_PICKUP")
    void updateOwnerOrderStatus_ShouldSucceed_ForValidSequence() {
        Order order = Order.builder()
                .id(666L)
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PLACED)
                .subtotalAmount(new BigDecimal("450.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("22.50"))
                .totalAmount(new BigDecimal("512.50"))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findWithDetailsById(666L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. PLACED -> ACCEPTED
        OrderResponse r1 = orderService.updateOwnerOrderStatus(666L, new OrderStatusUpdateRequest(OrderStatus.ACCEPTED), ownerDetails);
        assertThat(r1.status()).isEqualTo(OrderStatus.ACCEPTED);

        // 2. ACCEPTED -> PREPARING
        OrderResponse r2 = orderService.updateOwnerOrderStatus(666L, new OrderStatusUpdateRequest(OrderStatus.PREPARING), ownerDetails);
        assertThat(r2.status()).isEqualTo(OrderStatus.PREPARING);

        // 3. PREPARING -> READY_FOR_PICKUP
        OrderResponse r3 = orderService.updateOwnerOrderStatus(666L, new OrderStatusUpdateRequest(OrderStatus.READY_FOR_PICKUP), ownerDetails);
        assertThat(r3.status()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
    }

    @Test
    @DisplayName("Restaurant Owner transition should fail if owner does not own the restaurant")
    void updateOwnerOrderStatus_ShouldFail_WhenNotOwner() {
        User otherOwner = User.builder().id(99L).name("Other Owner").role(Role.RESTAURANT_OWNER).build();
        CustomUserDetails otherOwnerDetails = new CustomUserDetails(otherOwner);

        Order order = Order.builder()
                .id(666L)
                .customer(customer)
                .restaurant(restaurant) // Owned by owner (ID 2)
                .status(OrderStatus.PLACED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findWithDetailsById(666L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOwnerOrderStatus(666L, new OrderStatusUpdateRequest(OrderStatus.ACCEPTED), otherOwnerDetails))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Delivery Partner should successfully claim available order")
    void claimDeliveryOrder_ShouldSucceed_WhenAvailable() {
        Order claimedOrder = Order.builder()
                .id(555L)
                .customer(customer)
                .restaurant(restaurant)
                .deliveryPartner(deliveryPartner)
                .status(OrderStatus.READY_FOR_PICKUP)
                .subtotalAmount(new BigDecimal("450.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("22.50"))
                .totalAmount(new BigDecimal("512.50"))
                .items(new ArrayList<>())
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(deliveryPartner));
        when(orderRepository.claimOrder(eq(555L), eq(OrderStatus.READY_FOR_PICKUP), eq(deliveryPartner), any(Instant.class))).thenReturn(1);
        when(orderRepository.findWithDetailsById(555L)).thenReturn(Optional.of(claimedOrder));

        OrderResponse response = orderService.claimDeliveryOrder(555L, partnerDetails);

        assertThat(response).isNotNull();
        assertThat(response.deliveryPartnerId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Delivery Partner claim should fail with conflict when order was already claimed")
    void claimDeliveryOrder_ShouldFail_WhenAlreadyClaimed() {
        User otherPartner = User.builder().id(4L).name("Partner 2").build();
        Order existingOrder = Order.builder()
                .id(555L)
                .customer(customer)
                .restaurant(restaurant)
                .deliveryPartner(otherPartner)
                .status(OrderStatus.READY_FOR_PICKUP)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(deliveryPartner));
        when(orderRepository.claimOrder(eq(555L), eq(OrderStatus.READY_FOR_PICKUP), eq(deliveryPartner), any(Instant.class))).thenReturn(0);
        when(orderRepository.findById(555L)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.claimDeliveryOrder(555L, partnerDetails))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("already been claimed");
    }

    @Test
    @DisplayName("Delivery Partner should transition READY_FOR_PICKUP -> OUT_FOR_DELIVERY -> DELIVERED")
    void updateDeliveryOrderStatus_ShouldSucceed_ForAssignedPartner() {
        Order order = Order.builder()
                .id(444L)
                .customer(customer)
                .restaurant(restaurant)
                .deliveryPartner(deliveryPartner)
                .status(OrderStatus.READY_FOR_PICKUP)
                .subtotalAmount(new BigDecimal("450.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .taxAmount(new BigDecimal("22.50"))
                .totalAmount(new BigDecimal("512.50"))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findWithDetailsById(444L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. READY_FOR_PICKUP -> OUT_FOR_DELIVERY
        OrderResponse r1 = orderService.updateDeliveryOrderStatus(444L, new OrderStatusUpdateRequest(OrderStatus.OUT_FOR_DELIVERY), partnerDetails);
        assertThat(r1.status()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);

        // 2. OUT_FOR_DELIVERY -> DELIVERED
        OrderResponse r2 = orderService.updateDeliveryOrderStatus(444L, new OrderStatusUpdateRequest(OrderStatus.DELIVERED), partnerDetails);
        assertThat(r2.status()).isEqualTo(OrderStatus.DELIVERED);
    }
}
