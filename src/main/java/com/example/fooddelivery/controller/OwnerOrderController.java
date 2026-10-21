package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.OrderStatusUpdateRequest;
import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.dto.response.OrderResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.DeliveryService;
import com.example.fooddelivery.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Restaurant Owner order management endpoints.
 */
@RestController
@RequestMapping("/api/owner")
public class OwnerOrderController {

    private final OrderService orderService;
    private final DeliveryService deliveryService;

    public OwnerOrderController(OrderService orderService, DeliveryService deliveryService) {
        this.orderService = orderService;
        this.deliveryService = deliveryService;
    }

    /**
     * Retrieves paginated orders for a restaurant owned by the authenticated owner.
     *
     * @param restaurantId Restaurant ID
     * @param status Optional order status filter
     * @param currentUser Authenticated owner principal
     * @param pageable Pagination parameters
     * @return 200 OK with PagedResponse
     */
    @GetMapping("/restaurants/{restaurantId}/orders")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<PagedResponse<OrderResponse>> getRestaurantOrders(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<OrderResponse> response = orderService.getRestaurantOrders(restaurantId, status, pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves specific order details for an order at owner's restaurant.
     *
     * @param id Order ID
     * @param currentUser Authenticated owner principal
     * @return 200 OK with OrderResponse
     */
    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<OrderResponse> getOwnerOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse response = orderService.getOwnerOrderById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves delivery status and tracking details for an order at owner's restaurant.
     *
     * @param id Order ID
     * @param currentUser Authenticated owner principal
     * @return 200 OK with DeliveryStatusResponse
     */
    @GetMapping("/orders/{id}/delivery")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<DeliveryStatusResponse> getOwnerOrderDeliveryStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        DeliveryStatusResponse response = deliveryService.getOwnerOrderDeliveryStatus(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the status of an order for owner's restaurant (e.g. ACCEPTED, PREPARING, READY_FOR_PICKUP, CANCELLED).
     *
     * @param id Order ID
     * @param request OrderStatusUpdateRequest payload
     * @param currentUser Authenticated owner principal
     * @return 200 OK with updated OrderResponse
     */
    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<OrderResponse> updateOwnerOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse response = orderService.updateOwnerOrderStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
