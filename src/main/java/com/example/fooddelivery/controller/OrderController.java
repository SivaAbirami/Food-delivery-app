package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.CheckoutRequest;
import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.dto.response.OrderResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.DeliveryService;
import com.example.fooddelivery.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Customer order operations (checkout, history, detail, cancellation, delivery tracking).
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final DeliveryService deliveryService;

    public OrderController(OrderService orderService, DeliveryService deliveryService) {
        this.orderService = orderService;
        this.deliveryService = deliveryService;
    }

    /**
     * Converts the current customer's shopping cart into an Order.
     *
     * @param request CheckoutRequest containing address, instructions, and idempotency key
     * @param currentUser Authenticated customer principal
     * @return 201 CREATED with OrderResponse
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse response = orderService.checkout(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves paginated order history for the authenticated customer.
     *
     * @param currentUser Authenticated customer principal
     * @param pageable Pagination parameters (defaults to page 0, size 10, sort createdAt desc)
     * @return 200 OK with PagedResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PagedResponse<OrderResponse>> getCustomerOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<OrderResponse> response = orderService.getCustomerOrders(pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves specific order details for the authenticated participant (customer, owner, rider).
     *
     * @param id Order ID
     * @param currentUser Authenticated principal
     * @return 200 OK with OrderResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getCustomerOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse response = orderService.getCustomerOrderById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves delivery status and tracking details for the authenticated customer's order.
     *
     * @param id Order ID
     * @param currentUser Authenticated customer principal
     * @return 200 OK with DeliveryStatusResponse
     */
    @GetMapping("/{id}/delivery")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DeliveryStatusResponse> getCustomerOrderDeliveryStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        DeliveryStatusResponse response = deliveryService.getCustomerOrderDeliveryStatus(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an order before restaurant acceptance (status must be PLACED).
     *
     * @param id Order ID
     * @param currentUser Authenticated customer principal
     * @return 200 OK with updated OrderResponse
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> cancelCustomerOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse response = orderService.cancelCustomerOrder(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
