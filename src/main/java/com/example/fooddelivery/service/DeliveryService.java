package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.dto.response.DeliverySummaryResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Delivery and Dispatch workflows.
 */
public interface DeliveryService {

    PagedResponse<DeliverySummaryResponse> getAvailableDeliveries(
            Pageable pageable,
            CustomUserDetails currentUser
    );

    DeliverySummaryResponse claimDelivery(
            Long orderId,
            CustomUserDetails currentUser
    );

    PagedResponse<DeliverySummaryResponse> getMyDeliveries(
            Pageable pageable,
            CustomUserDetails currentUser
    );

    DeliveryStatusResponse pickupDelivery(
            Long orderId,
            CustomUserDetails currentUser
    );

    DeliveryStatusResponse completeDelivery(
            Long orderId,
            CustomUserDetails currentUser
    );

    DeliveryStatusResponse getCustomerOrderDeliveryStatus(
            Long orderId,
            CustomUserDetails currentUser
    );

    DeliveryStatusResponse getOwnerOrderDeliveryStatus(
            Long orderId,
            CustomUserDetails currentUser
    );
}
