package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.UpdateDeliveryLocationRequest;
import com.example.fooddelivery.dto.response.DeliveryLocationResponse;
import com.example.fooddelivery.security.CustomUserDetails;

public interface DeliveryTrackingService {
    void updateLocation(Long orderId, UpdateDeliveryLocationRequest request, CustomUserDetails currentUser);
    DeliveryLocationResponse getLiveLocation(Long orderId);
    void verifyAndCompleteDelivery(Long orderId, String otp, CustomUserDetails currentUser);
}

