package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.CreateOfferRequest;
import com.example.fooddelivery.dto.request.ValidateCouponRequest;
import com.example.fooddelivery.dto.response.CouponValidationResponse;
import com.example.fooddelivery.dto.response.OfferResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * Public endpoint to get all active promotional banners and platform-wide offers.
     */
    @GetMapping("/offers")
    public ResponseEntity<List<OfferResponse>> getActiveOffers() {
        List<OfferResponse> offers = offerService.getActivePlatformOffers();
        return ResponseEntity.ok(offers);
    }

    /**
     * Public endpoint to get all active offers valid for a specific restaurant.
     */
    @GetMapping("/restaurants/{restaurantId}/offers")
    public ResponseEntity<List<OfferResponse>> getRestaurantOffers(@PathVariable Long restaurantId) {
        List<OfferResponse> offers = offerService.getOffersForRestaurant(restaurantId);
        return ResponseEntity.ok(offers);
    }

    /**
     * Public/Customer endpoint to dynamically validate a coupon code against subtotal and restaurant.
     */
    @PostMapping("/offers/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(@Valid @RequestBody ValidateCouponRequest request) {
        CouponValidationResponse response = offerService.validateCoupon(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Merchant endpoint for Restaurant Owners to create dynamic offers and deals for their restaurant!
     */
    @PostMapping("/owner/restaurants/{restaurantId}/offers")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<OfferResponse> createRestaurantOffer(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateOfferRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OfferResponse response = offerService.createRestaurantOffer(restaurantId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

