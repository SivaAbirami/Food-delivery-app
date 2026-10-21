package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.CheckoutRequest;
import com.example.fooddelivery.dto.request.OrderStatusUpdateRequest;
import com.example.fooddelivery.dto.response.OrderResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;

/**
 * Service interface defining order management and checkout operations across all roles.
 */
public interface OrderService {

    // Customer operations
    OrderResponse checkout(CheckoutRequest request, CustomUserDetails currentUser);

    PagedResponse<OrderResponse> getCustomerOrders(Pageable pageable, CustomUserDetails currentUser);

    OrderResponse getCustomerOrderById(Long orderId, CustomUserDetails currentUser);

    OrderResponse cancelCustomerOrder(Long orderId, CustomUserDetails currentUser);

    // Restaurant Owner operations
    PagedResponse<OrderResponse> getRestaurantOrders(Long restaurantId, OrderStatus status, Pageable pageable, CustomUserDetails currentUser);

    OrderResponse getOwnerOrderById(Long orderId, CustomUserDetails currentUser);

    OrderResponse updateOwnerOrderStatus(Long orderId, OrderStatusUpdateRequest request, CustomUserDetails currentUser);

    // Delivery Partner operations
    PagedResponse<OrderResponse> getAvailableDeliveryOrders(Pageable pageable, CustomUserDetails currentUser);

    OrderResponse claimDeliveryOrder(Long orderId, CustomUserDetails currentUser);

    PagedResponse<OrderResponse> getMyDeliveryOrders(Pageable pageable, CustomUserDetails currentUser);

    OrderResponse updateDeliveryOrderStatus(Long orderId, OrderStatusUpdateRequest request, CustomUserDetails currentUser);
}
