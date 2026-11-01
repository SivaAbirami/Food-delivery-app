package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.UpdateDeliveryLocationRequest;
import com.example.fooddelivery.dto.response.DeliveryLocationResponse;
import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.DeliveryTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller handling live GPS telemetry streaming and delivery OTP verification.
 */
@RestController
@RequestMapping("/api")
public class DeliveryLocationController {

    private final DeliveryTrackingService deliveryTrackingService;

    public DeliveryLocationController(DeliveryTrackingService deliveryTrackingService) {
        this.deliveryTrackingService = deliveryTrackingService;
    }

    /**
     * Broadcast live GPS coordinates for an order (Delivery Partner or Simulator).
     */
    @PostMapping("/deliveries/{orderId}/location")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateDeliveryLocationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        deliveryTrackingService.updateLocation(orderId, request, currentUser);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "orderId", orderId,
                "updatedAt", System.currentTimeMillis()
        ));
    }

    /**
     * Fetch live location of the delivery agent for an order (Customer tracking map).
     */
    @GetMapping("/orders/{orderId}/delivery-location")
    public ResponseEntity<DeliveryLocationResponse> getLiveLocation(@PathVariable Long orderId) {
        DeliveryLocationResponse response = deliveryTrackingService.getLiveLocation(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify 4-digit OTP and complete delivery.
     */
    @PostMapping("/deliveries/{orderId}/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtpAndComplete(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        String otp = payload.get("otp");
        deliveryTrackingService.verifyAndCompleteDelivery(orderId, otp, currentUser);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Order delivered and verified successfully!",
                "orderId", orderId
        ));
    }
}

