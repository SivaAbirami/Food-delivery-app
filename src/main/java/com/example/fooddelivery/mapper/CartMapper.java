package com.example.fooddelivery.mapper;

import com.example.fooddelivery.dto.response.CartItemResponse;
import com.example.fooddelivery.dto.response.CartResponse;
import com.example.fooddelivery.entity.Cart;
import com.example.fooddelivery.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting Cart and CartItem JPA entities into API response DTOs.
 */
@Component
public class CartMapper {

    /**
     * Maps CartItem entity to CartItemResponse DTO calculating subtotal.
     *
     * @param item persistent cart item entity
     * @return CartItemResponse DTO
     */
    public CartItemResponse toItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }

        Long menuItemId = (item.getMenuItem() != null) ? item.getMenuItem().getId() : null;
        String menuItemName = (item.getMenuItem() != null) ? item.getMenuItem().getName() : null;
        String imageUrl = (item.getMenuItem() != null) ? item.getMenuItem().getImageUrl() : null;
        BigDecimal unitPrice = item.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getId(),
                menuItemId,
                menuItemName,
                unitPrice,
                item.getQuantity(),
                subtotal,
                imageUrl
        );
    }

    /**
     * Maps Cart entity to CartResponse DTO calculating aggregated total item count and total cart amount.
     *
     * @param cart persistent cart entity
     * @return CartResponse DTO
     */
    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        Long restaurantId = (cart.getRestaurant() != null) ? cart.getRestaurant().getId() : null;
        String restaurantName = (cart.getRestaurant() != null) ? cart.getRestaurant().getName() : null;

        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalItemCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                CartItemResponse itemResp = toItemResponse(item);
                if (itemResp != null) {
                    itemResponses.add(itemResp);
                    totalItemCount += itemResp.quantity();
                    totalAmount = totalAmount.add(itemResp.subtotal());
                }
            }
        }

        return new CartResponse(
                cart.getId(),
                restaurantId,
                restaurantName,
                itemResponses,
                totalItemCount,
                totalAmount,
                cart.getUpdatedAt() != null ? cart.getUpdatedAt() : cart.getCreatedAt()
        );
    }
}
