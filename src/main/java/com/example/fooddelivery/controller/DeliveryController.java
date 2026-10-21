package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.dto.response.DeliverySummaryResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.DeliveryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Delivery & Dispatch operations (/api/deliveries).
 */
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /**
     * Lists claimable orders in READY_FOR_PICKUP status without an assigned driver.
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<PagedResponse<DeliverySummaryResponse>> getAvailableDeliveries(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PagedResponse<DeliverySummaryResponse> response = deliveryService.getAvailableDeliveries(pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Concurrency-safe atomic claim of a ready-for-pickup delivery by authenticated delivery partner.
     */
    @PostMapping("/{orderId}/claim")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<DeliverySummaryResponse> claimDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        DeliverySummaryResponse response = deliveryService.claimDelivery(orderId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists deliveries assigned to the authenticated delivery partner.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<PagedResponse<DeliverySummaryResponse>> getMyDeliveries(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<DeliverySummaryResponse> response = deliveryService.getMyDeliveries(pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Transitions status from READY_FOR_PICKUP to OUT_FOR_DELIVERY.
     */
    @PatchMapping("/{orderId}/pickup")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<DeliveryStatusResponse> pickupDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        DeliveryStatusResponse response = deliveryService.pickupDelivery(orderId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Transitions status from OUT_FOR_DELIVERY to DELIVERED.
     */
    @PatchMapping("/{orderId}/deliver")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<DeliveryStatusResponse> completeDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        DeliveryStatusResponse response = deliveryService.completeDelivery(orderId, currentUser);
        return ResponseEntity.ok(response);
    }
}
