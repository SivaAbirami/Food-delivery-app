package com.example.fooddelivery.mapper;

import com.example.fooddelivery.dto.response.RestaurantResponse;
import com.example.fooddelivery.entity.Restaurant;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Restaurant JPA entities into RestaurantResponse DTOs.
 */
@Component
public class RestaurantMapper {

    /**
     * Maps Restaurant entity to RestaurantResponse DTO.
     * Note: Accessing owner fields (id, name) must occur within a valid transactional boundary
     * or active persistence context to avoid LazyInitializationException.
     *
     * @param restaurant persistent entity
     * @return RestaurantResponse DTO
     */
    public RestaurantResponse toResponse(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }

        Long ownerId = (restaurant.getOwner() != null) ? restaurant.getOwner().getId() : null;
        String ownerName = (restaurant.getOwner() != null) ? restaurant.getOwner().getName() : null;

        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.isOpen(),
                ownerId,
                ownerName,
                restaurant.getCreatedAt()
        );
    }
}
