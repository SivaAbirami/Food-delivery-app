package com.example.fooddelivery.service.impl;

import com.example.fooddelivery.dto.request.CreateOfferRequest;
import com.example.fooddelivery.dto.request.ValidateCouponRequest;
import com.example.fooddelivery.dto.response.CouponValidationResponse;
import com.example.fooddelivery.dto.response.OfferResponse;
import com.example.fooddelivery.entity.Offer;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.ConflictException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.repository.OfferRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.OfferService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final RestaurantRepository restaurantRepository;

    public OfferServiceImpl(OfferRepository offerRepository, RestaurantRepository restaurantRepository) {
        this.offerRepository = offerRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public List<OfferResponse> getActivePlatformOffers() {
        return offerRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<OfferResponse> getOffersForRestaurant(Long restaurantId) {
        return offerRepository.findActiveOffersForRestaurant(restaurantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OfferResponse createRestaurantOffer(Long restaurantId, CreateOfferRequest request, CustomUserDetails currentUser) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        // Enforce ownership security
        if (!restaurant.getOwner().getId().equals(currentUser.getUser().getId())) {
            throw new AccessDeniedException("You do not own this restaurant");
        }

        String normalizedCode = request.code().trim().toUpperCase();
        if (offerRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ConflictException("Coupon code '" + normalizedCode + "' already exists");
        }

        Offer offer = Offer.builder()
                .code(normalizedCode)
                .title(request.title().trim())
                .description(request.description())
                .discountPercentage(request.discountPercentage())
                .maxDiscountAmount(request.maxDiscountAmount())
                .minOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO)
                .restaurant(restaurant)
                .bannerImageUrl(request.bannerImageUrl())
                .gradientColor(request.gradientColor() != null ? request.gradientColor() : "from-orange-600 to-amber-500")
                .isActive(true)
                .build();

        Offer saved = offerRepository.save(offer);
        return toResponse(saved);
    }

    @Override
    public CouponValidationResponse validateCoupon(ValidateCouponRequest request) {
        Optional<Offer> offerOpt = offerRepository.findByCodeIgnoreCaseAndIsActiveTrue(request.code().trim());
        if (offerOpt.isEmpty()) {
            return new CouponValidationResponse(false, "Invalid or expired coupon code", request.code(), BigDecimal.ZERO, request.subtotal());
        }

        Offer offer = offerOpt.get();

        // Check restaurant restriction
        if (offer.getRestaurant() != null) {
            if (request.restaurantId() == null || !offer.getRestaurant().getId().equals(request.restaurantId())) {
                return new CouponValidationResponse(
                        false,
                        "This coupon is only valid for orders from " + offer.getRestaurant().getName(),
                        offer.getCode(),
                        BigDecimal.ZERO,
                        request.subtotal()
                );
            }
        }

        // Check minimum order amount
        if (offer.getMinOrderAmount() != null && request.subtotal().compareTo(offer.getMinOrderAmount()) < 0) {
            return new CouponValidationResponse(
                    false,
                    "Order amount must be at least ₹" + offer.getMinOrderAmount() + " to use this coupon",
                    offer.getCode(),
                    BigDecimal.ZERO,
                    request.subtotal()
            );
        }

        // Calculate discount
        BigDecimal percentage = BigDecimal.valueOf(offer.getDiscountPercentage()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal discount = request.subtotal().multiply(percentage).setScale(2, RoundingMode.HALF_UP);

        if (offer.getMaxDiscountAmount() != null && discount.compareTo(offer.getMaxDiscountAmount()) > 0) {
            discount = offer.getMaxDiscountAmount();
        }

        BigDecimal finalTotal = request.subtotal().subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        return new CouponValidationResponse(
                true,
                "Coupon applied! You saved ₹" + discount,
                offer.getCode(),
                discount,
                finalTotal
        );
    }

    private OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getCode(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getDiscountPercentage(),
                offer.getMaxDiscountAmount(),
                offer.getMinOrderAmount(),
                offer.getRestaurant() != null ? offer.getRestaurant().getId() : null,
                offer.getRestaurant() != null ? offer.getRestaurant().getName() : null,
                offer.getBannerImageUrl(),
                offer.getGradientColor(),
                offer.isActive()
        );
    }
}

