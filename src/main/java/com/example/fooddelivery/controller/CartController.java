package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.AddToCartRequest;
import com.example.fooddelivery.dto.request.UpdateCartItemRequest;
import com.example.fooddelivery.dto.response.CartResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for customer shopping cart operations.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Retrieves the current customer's shopping cart.
     *
     * @param currentUser Authenticated customer principal
     * @return 200 OK with CartResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CartResponse response = cartService.getCart(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds an item to the customer's cart or increments its quantity.
     *
     * @param request AddToCartRequest payload
     * @param currentUser Authenticated customer principal
     * @return 200 OK with updated CartResponse
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CartResponse response = cartService.addItemToCart(request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the quantity of a specific cart item line.
     *
     * @param cartItemId CartItem ID
     * @param request UpdateCartItemRequest payload
     * @param currentUser Authenticated customer principal
     * @return 200 OK with updated CartResponse
     */
    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CartResponse response = cartService.updateCartItemQuantity(cartItemId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes a specific item from the cart.
     *
     * @param cartItemId CartItem ID
     * @param currentUser Authenticated customer principal
     * @return 200 OK with updated CartResponse
     */
    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CartResponse response = cartService.removeCartItem(cartItemId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Clears all items from the customer's cart.
     *
     * @param currentUser Authenticated customer principal
     * @return 204 NO_CONTENT
     */
    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        cartService.clearCart(currentUser);
        return ResponseEntity.noContent().build();
    }
}
