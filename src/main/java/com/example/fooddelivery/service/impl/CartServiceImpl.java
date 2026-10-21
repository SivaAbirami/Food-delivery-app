package com.example.fooddelivery.service.impl;

import com.example.fooddelivery.dto.request.AddToCartRequest;
import com.example.fooddelivery.dto.request.UpdateCartItemRequest;
import com.example.fooddelivery.dto.response.CartResponse;
import com.example.fooddelivery.entity.Cart;
import com.example.fooddelivery.entity.CartItem;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.CartConflictException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.CartMapper;
import com.example.fooddelivery.repository.CartItemRepository;
import com.example.fooddelivery.repository.CartRepository;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.CartService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Production implementation of CartService.
 */
@Service
public class CartServiceImpl implements CartService {

    private static final int MAX_QUANTITY_PER_ITEM = 50;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    public CartServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            MenuItemRepository menuItemRepository,
            UserRepository userRepository,
            CartMapper cartMapper
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
    }

    @Override
    @Transactional
    public CartResponse getCart(CustomUserDetails currentUser) {
        validateCustomerRole(currentUser);
        User customer = loadCustomer(currentUser.getId());
        Cart cart = getOrCreateCart(customer);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(AddToCartRequest request, CustomUserDetails currentUser) {
        validateCustomerRole(currentUser);
        User customer = loadCustomer(currentUser.getId());

        MenuItem menuItem = menuItemRepository.findById(request.menuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + request.menuItemId()));

        // Check availability
        if (!menuItem.isAvailable()) {
            throw new IllegalArgumentException("Menu item '" + menuItem.getName() + "' is currently unavailable");
        }

        // Check restaurant status
        if (menuItem.getRestaurant() == null || !menuItem.getRestaurant().isOpen()) {
            throw new IllegalArgumentException("Restaurant '" + (menuItem.getRestaurant() != null ? menuItem.getRestaurant().getName() : "Unknown") + "' is currently closed");
        }

        Cart cart = getOrCreateCart(customer);

        // Enforce single-restaurant invariant (409 Conflict)
        if (cart.getRestaurant() != null && !cart.getRestaurant().getId().equals(menuItem.getRestaurant().getId())) {
            throw new CartConflictException("Cart already contains items from another restaurant ('" + cart.getRestaurant().getName() + "'). Please clear your cart before adding items from '" + menuItem.getRestaurant().getName() + "'.");
        }

        // Associate restaurant if not yet set
        if (cart.getRestaurant() == null) {
            cart.setRestaurant(menuItem.getRestaurant());
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(menuItem.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.quantity();
            if (newQuantity > MAX_QUANTITY_PER_ITEM) {
                throw new IllegalArgumentException("Maximum quantity allowed per item is " + MAX_QUANTITY_PER_ITEM);
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setPrice(menuItem.getPrice()); // Refresh price snapshot
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .menuItem(menuItem)
                    .quantity(request.quantity())
                    .price(menuItem.getPrice()) // Read authoritative current price from MenuItem
                    .build();
            cart.addItem(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(Long cartItemId, UpdateCartItemRequest request, CustomUserDetails currentUser) {
        validateCustomerRole(currentUser);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        verifyCartOwnership(cartItem.getCart(), currentUser);

        if (request.quantity() > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException("Maximum quantity allowed per item is " + MAX_QUANTITY_PER_ITEM);
        }

        cartItem.setQuantity(request.quantity());
        cartItem.setPrice(cartItem.getMenuItem().getPrice()); // Refresh price snapshot

        cartItemRepository.save(cartItem);
        return cartMapper.toResponse(cartItem.getCart());
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long cartItemId, CustomUserDetails currentUser) {
        validateCustomerRole(currentUser);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Cart cart = cartItem.getCart();
        verifyCartOwnership(cart, currentUser);

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);
        Cart savedCart = cartRepository.save(cart);

        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public void clearCart(CustomUserDetails currentUser) {
        validateCustomerRole(currentUser);
        User customer = loadCustomer(currentUser.getId());
        cartRepository.findByCustomerId(customer.getId()).ifPresent(cart -> {
            cart.clearItems();
            cartRepository.save(cart);
        });
    }

    private Cart getOrCreateCart(User customer) {
        return cartRepository.findWithDetailsByCustomerId(customer.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .customer(customer)
                            .restaurant(null)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private User loadCustomer(Long customerId) {
        return userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found"));
    }

    private void validateCustomerRole(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (!Role.CUSTOMER.equals(currentUser.getUser().getRole())) {
            throw new AccessDeniedException("User must have CUSTOMER role to perform cart operations");
        }
    }

    private void verifyCartOwnership(Cart cart, CustomUserDetails currentUser) {
        validateCustomerRole(currentUser);
        if (cart.getCustomer() == null || !cart.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this cart");
        }
    }
}
