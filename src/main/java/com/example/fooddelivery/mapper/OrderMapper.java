package com.example.fooddelivery.mapper;

import com.example.fooddelivery.dto.response.OrderItemResponse;
import com.example.fooddelivery.dto.response.OrderResponse;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting Order and OrderItem JPA entities into API response DTOs.
 */
@Component
public class OrderMapper {

    public OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        Long menuItemId = (item.getMenuItem() != null) ? item.getMenuItem().getId() : null;

        return new OrderItemResponse(
                item.getId(),
                menuItemId,
                item.getItemName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        Long customerId = (order.getCustomer() != null) ? order.getCustomer().getId() : null;
        Long restaurantId = (order.getRestaurant() != null) ? order.getRestaurant().getId() : null;
        String restaurantName = (order.getRestaurant() != null) ? order.getRestaurant().getName() : null;
        Long deliveryPartnerId = (order.getDeliveryPartner() != null) ? order.getDeliveryPartner().getId() : null;

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemResponse itemResp = toItemResponse(item);
                if (itemResp != null) {
                    itemResponses.add(itemResp);
                }
            }
        }

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                customerId,
                restaurantId,
                restaurantName,
                deliveryPartnerId,
                order.getStatus(),
                order.getDeliveryAddress(),
                order.getSpecialInstructions(),
                itemResponses,
                order.getSubtotalAmount(),
                order.getDeliveryFee(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getDeliveryOtp(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
