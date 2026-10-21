package com.example.fooddelivery.mapper;

import com.example.fooddelivery.dto.request.MenuItemRequest;
import com.example.fooddelivery.dto.response.MenuItemResponse;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting MenuItem JPA entities into MenuItemResponse DTOs
 * and MenuItemRequest DTOs into MenuItem entities.
 */
@Component
public class MenuItemMapper {

    /**
     * Maps MenuItem entity to MenuItemResponse DTO.
     *
     * @param menuItem persistent entity
     * @return MenuItemResponse DTO
     */
    public MenuItemResponse toResponse(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }

        Long restaurantId = (menuItem.getRestaurant() != null) ? menuItem.getRestaurant().getId() : null;
        String restaurantName = (menuItem.getRestaurant() != null) ? menuItem.getRestaurant().getName() : null;

        return new MenuItemResponse(
                menuItem.getId(),
                restaurantId,
                restaurantName,
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getCategory(),
                menuItem.isAvailable(),
                menuItem.getImageUrl(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt()
        );
    }

    /**
     * Maps MenuItemRequest DTO and managed Restaurant entity to a new MenuItem entity.
     *
     * @param request validated request payload
     * @param restaurant parent restaurant entity
     * @return unpersisted MenuItem entity with default availability set to true
     */
    public MenuItem toEntity(MenuItemRequest request, Restaurant restaurant) {
        if (request == null) {
            return null;
        }

        return MenuItem.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .price(request.price())
                .category(request.category().trim())
                .imageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null)
                .restaurant(restaurant)
                .isAvailable(true)
                .build();
    }
}
