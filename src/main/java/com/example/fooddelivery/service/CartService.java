package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.AddToCartRequest;
import com.example.fooddelivery.dto.request.UpdateCartItemRequest;
import com.example.fooddelivery.dto.response.CartResponse;
import com.example.fooddelivery.security.CustomUserDetails;

/**
 * Service contract for customer shopping cart operations.
 */
public interface CartService {

    /**
     * Retrieves the authenticated customer's shopping cart.
     * If the customer doesn't have an active cart yet, initializes and returns an empty cart.
     *
     * @param currentUser Authenticated customer principal
     * @return CartResponse DTO
     */
    CartResponse getCart(CustomUserDetails currentUser);

    /**
     * Adds a menu item to the customer's cart or increments its quantity if already present.
     * Enforces single-restaurant invariant, item availability, and current-price extraction.
     *
     * @param request AddToCartRequest payload
     * @param currentUser Authenticated customer principal
     * @return Updated CartResponse DTO
     */
    CartResponse addItemToCart(AddToCartRequest request, CustomUserDetails currentUser);

    /**
     * Updates the quantity of a specific cart item line.
     *
     * @param cartItemId ID of the CartItem line to update
     * @param request UpdateCartItemRequest payload containing new quantity
     * @param currentUser Authenticated customer principal
     * @return Updated CartResponse DTO
     */
    CartResponse updateCartItemQuantity(Long cartItemId, UpdateCartItemRequest request, CustomUserDetails currentUser);

    /**
     * Removes a specific item from the cart.
     * Resets the cart's restaurant reference if it becomes empty.
     *
     * @param cartItemId ID of the CartItem line to remove
     * @param currentUser Authenticated customer principal
     * @return Updated CartResponse DTO
     */
    CartResponse removeCartItem(Long cartItemId, CustomUserDetails currentUser);

    /**
     * Clears all items from the customer's cart and resets the restaurant reference.
     *
     * @param currentUser Authenticated customer principal
     */
    void clearCart(CustomUserDetails currentUser);
}
