package com.example.fooddelivery.service.impl;

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
import com.example.fooddelivery.enums.OrderStatusTransitionValidator;
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
import com.example.fooddelivery.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Order Management and Checkout.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final BigDecimal DELIVERY_FEE = BigDecimal.valueOf(40.00).setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.05); // 5% GST
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            MenuItemRepository menuItemRepository,
            RestaurantRepository restaurantRepository,
            UserRepository userRepository,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional
    public OrderResponse checkout(CheckoutRequest request, CustomUserDetails currentUser) {
        log.info("Processing checkout for customer ID: {}", currentUser.getId());

        // 1. Idempotency Check: if key is provided and already exists, return existing order safely
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Order> existingOrderOpt = orderRepository.findByIdempotencyKey(request.idempotencyKey().trim());
            if (existingOrderOpt.isPresent()) {
                Order existingOrder = existingOrderOpt.get();
                if (!existingOrder.getCustomer().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Idempotency key does not belong to authenticated user");
                }
                log.info("Idempotent checkout triggered for customer ID: {}. Returning existing order ID: {}",
                        currentUser.getId(), existingOrder.getId());
                return orderMapper.toResponse(existingOrder);
            }
        }

        // 2. Fetch authenticated Customer
        User customer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + currentUser.getId()));

        // 3. Fetch Customer's Cart with Details
        Cart cart = cartRepository.findWithDetailsByCustomerId(customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Shopping cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Shopping cart is empty");
        }

        Restaurant restaurant = cart.getRestaurant();
        if (restaurant == null) {
            throw new IllegalArgumentException("Shopping cart has no associated restaurant");
        }

        // 4. Validate Restaurant is currently open
        if (!restaurant.isOpen()) {
            throw new IllegalArgumentException(
                    String.format("Restaurant '%s' is currently closed and not accepting orders", restaurant.getName())
            );
        }

        // 5. Re-read all MenuItems from Database for Authoritative Price & Availability Snapshot
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(cartItem.getMenuItem().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Menu item with ID " + cartItem.getMenuItem().getId() + " no longer exists"
                    ));

            if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException(
                        String.format("Menu item '%s' does not belong to restaurant '%s'", menuItem.getName(), restaurant.getName())
                );
            }

            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException(
                        String.format("Menu item '%s' is currently unavailable", menuItem.getName())
                );
            }

            // AUTHORITATIVE PRICE SNAPSHOT: Read live MenuItem price, ignoring CartItem.price
            BigDecimal authoritativePrice = menuItem.getPrice();
            BigDecimal itemSubtotal = authoritativePrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .itemName(menuItem.getName())
                    .price(authoritativePrice)
                    .quantity(cartItem.getQuantity())
                    .subtotal(itemSubtotal)
                    .build();

            orderItems.add(orderItem);
            subtotalAmount = subtotalAmount.add(itemSubtotal);
        }

        subtotalAmount = subtotalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal deliveryFee = DELIVERY_FEE;
        BigDecimal taxAmount = subtotalAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotalAmount.add(deliveryFee).add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        // 6. Generate Unique Order Number
        String orderNumber = generateUniqueOrderNumber();

        // 7. Build and Persist Order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PLACED)
                .deliveryAddress(request.deliveryAddress())
                .specialInstructions(request.specialInstructions())
                .subtotalAmount(subtotalAmount)
                .deliveryFee(deliveryFee)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .idempotencyKey((request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                        ? request.idempotencyKey().trim() : null)
                .build();

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        try {
            order = orderRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent race on idempotencyKey
            if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
                Optional<Order> racedOrder = orderRepository.findByIdempotencyKey(request.idempotencyKey().trim());
                if (racedOrder.isPresent()) {
                    log.info("Concurrent idempotency race won by another thread for key: {}", request.idempotencyKey());
                    return orderMapper.toResponse(racedOrder.get());
                }
            }
            throw ex;
        }

        // 8. Atomically Clear Cart in same database transaction
        cart.clearItems();
        cartRepository.save(cart);

        log.info("Successfully created order: {} (ID: {}) for customer ID: {}", order.getOrderNumber(), order.getId(), customer.getId());
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getCustomerOrders(Pageable pageable, CustomUserDetails currentUser) {
        Page<Order> orders = orderRepository.findByCustomerId(currentUser.getId(), pageable);
        return PagedResponse.from(orders.map(orderMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderById(Long orderId, CustomUserDetails currentUser) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelCustomerOrder(Long orderId, CustomUserDetails currentUser) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to cancel this order");
        }

        OrderStatusTransitionValidator.validateCustomerCancellation(order.getStatus());

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        log.info("Order ID: {} cancelled by customer ID: {}", order.getId(), currentUser.getId());
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getRestaurantOrders(
            Long restaurantId,
            OrderStatus status,
            Pageable pageable,
            CustomUserDetails currentUser
    ) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + restaurantId));

        if (!restaurant.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view orders for this restaurant");
        }

        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByRestaurantIdAndStatus(restaurantId, status, pageable);
        } else {
            orders = orderRepository.findByRestaurantId(restaurantId, pageable);
        }

        return PagedResponse.from(orders.map(orderMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOwnerOrderById(Long orderId, CustomUserDetails currentUser) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOwnerOrderStatus(Long orderId, OrderStatusUpdateRequest request, CustomUserDetails currentUser) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to update status for this order");
        }

        OrderStatusTransitionValidator.validateOwnerTransition(order.getStatus(), request.status());

        order.setStatus(request.status());
        order = orderRepository.save(order);

        log.info("Order ID: {} status updated to '{}' by owner ID: {}", order.getId(), request.status(), currentUser.getId());
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAvailableDeliveryOrders(Pageable pageable, CustomUserDetails currentUser) {
        Page<Order> orders = orderRepository.findByStatusAndDeliveryPartnerIsNull(OrderStatus.READY_FOR_PICKUP, pageable);
        return PagedResponse.from(orders.map(orderMapper::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse claimDeliveryOrder(Long orderId, CustomUserDetails currentUser) {
        User partner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with ID: " + currentUser.getId()));

        // Concurrency-safe atomic database update
        int rowsUpdated = orderRepository.claimOrder(orderId, OrderStatus.READY_FOR_PICKUP, partner, Instant.now());

        if (rowsUpdated == 0) {
            Order existingOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

            if (existingOrder.getStatus() != OrderStatus.READY_FOR_PICKUP) {
                throw new IllegalOrderStateTransitionException(
                        String.format("Order is in status '%s' and is not available for pickup", existingOrder.getStatus())
                );
            }

            if (existingOrder.getDeliveryPartner() != null) {
                throw new OrderConflictException("Order has already been claimed by another delivery partner");
            }

            throw new OrderConflictException("Could not claim order due to concurrent modification");
        }

        Order claimedOrder = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        log.info("Order ID: {} successfully claimed by delivery partner ID: {}", orderId, partner.getId());
        return orderMapper.toResponse(claimedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getMyDeliveryOrders(Pageable pageable, CustomUserDetails currentUser) {
        Page<Order> orders = orderRepository.findByDeliveryPartnerId(currentUser.getId(), pageable);
        return PagedResponse.from(orders.map(orderMapper::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse updateDeliveryOrderStatus(Long orderId, OrderStatusUpdateRequest request, CustomUserDetails currentUser) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getDeliveryPartner() == null || !order.getDeliveryPartner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not the assigned delivery partner for this order");
        }

        OrderStatusTransitionValidator.validateDeliveryPartnerTransition(order.getStatus(), request.status());

        order.setStatus(request.status());
        order = orderRepository.save(order);

        log.info("Order ID: {} status updated to '{}' by delivery partner ID: {}", order.getId(), request.status(), currentUser.getId());
        return orderMapper.toResponse(order);
    }

    private String generateUniqueOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return "ORD-" + datePrefix + "-" + suffix;
    }
}
