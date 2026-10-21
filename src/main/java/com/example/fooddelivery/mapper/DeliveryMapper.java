package com.example.fooddelivery.mapper;

import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.dto.response.DeliverySummaryResponse;
import com.example.fooddelivery.entity.Order;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Order entities into Delivery DTOs.
 */
@Component
public class DeliveryMapper {

    public DeliverySummaryResponse toSummaryResponse(Order order) {
        if (order == null) {
            return null;
        }

        Long restaurantId = (order.getRestaurant() != null) ? order.getRestaurant().getId() : null;
        String restaurantName = (order.getRestaurant() != null) ? order.getRestaurant().getName() : null;
        String restaurantAddress = (order.getRestaurant() != null) ? order.getRestaurant().getAddress() : null;
        String restaurantPhone = (order.getRestaurant() != null) ? order.getRestaurant().getPhone() : null;

        return new DeliverySummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                restaurantId,
                restaurantName,
                restaurantAddress,
                restaurantPhone,
                order.getDeliveryAddress(),
                order.getSpecialInstructions(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public DeliveryStatusResponse toStatusResponse(Order order) {
        if (order == null) {
            return null;
        }

        boolean assigned = (order.getDeliveryPartner() != null);
        String driverName = assigned ? order.getDeliveryPartner().getName() : null;
        String restaurantName = (order.getRestaurant() != null) ? order.getRestaurant().getName() : null;

        return new DeliveryStatusResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                assigned,
                driverName,
                restaurantName,
                order.getDeliveryAddress(),
                order.getUpdatedAt()
        );
    }
}
