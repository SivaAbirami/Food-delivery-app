package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.CreateOfferRequest;
import com.example.fooddelivery.dto.request.ValidateCouponRequest;
import com.example.fooddelivery.dto.response.CouponValidationResponse;
import com.example.fooddelivery.dto.response.OfferResponse;
import com.example.fooddelivery.security.CustomUserDetails;

import java.util.List;

public interface OfferService {

    List<OfferResponse> getActivePlatformOffers();

    List<OfferResponse> getOffersForRestaurant(Long restaurantId);

    OfferResponse createRestaurantOffer(Long restaurantId, CreateOfferRequest request, CustomUserDetails currentUser);

    CouponValidationResponse validateCoupon(ValidateCouponRequest request);
}

