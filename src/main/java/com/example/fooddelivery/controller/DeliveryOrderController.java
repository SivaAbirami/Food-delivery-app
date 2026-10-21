package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.OrderStatusUpdateRequest;
import com.example.fooddelivery.dto.response.OrderResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.security.CustomUserDetails;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Delivery Partner order discovery, claiming, and lifecycle progression.
 */
@RestController
@RequestMapping("/api/delivery")
public class DeliveryOrderController {

    private final OrderService orderService;

    public DeliveryOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Lists orders that are READY_FOR_PICKUP and not yet claimed by any delivery partner.
     *
     * @param currentUser Authenticated delivery partner principal
     * @param pageable Pagination parameters
     * @return 200 OK with PagedResponse
     */
    @GetMapping("/orders/available")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<PagedResponse<OrderResponse>> getAvailableOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PagedResponse<OrderResponse> response = orderService.getAvailableDeliveryOrders(pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Concurrency-safe claiming of an available order by authenticated delivery partner.
     *
     * @param id Order ID
     * @param currentUser Authenticated delivery partner principal
     * @return 200 OK with updated OrderResponse
     */
    @PatchMapping("/orders/{id}/claim")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<OrderResponse> claimOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse response = orderService.claimDeliveryOrder(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists orders claimed by and assigned to the authenticated delivery partner.
     *
     * @param currentUser Authenticated delivery partner principal
     * @param pageable Pagination parameters
     * @return 200 OK with PagedResponse
     */
    @GetMapping("/orders/my")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<PagedResponse<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<OrderResponse> response = orderService.getMyDeliveryOrders(pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Transitions status of an assigned order (e.g. READY_FOR_PICKUP -> OUT_FOR_DELIVERY -> DELIVERED).
     *
     * @param id Order ID
     * @param request OrderStatusUpdateRequest payload
     * @param currentUser Authenticated delivery partner principal
     * @return 200 OK with updated OrderResponse
     */
    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse response = orderService.updateDeliveryOrderStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
