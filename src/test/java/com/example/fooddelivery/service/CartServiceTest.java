package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.AddToCartRequest;
import com.example.fooddelivery.dto.request.UpdateCartItemRequest;
import com.example.fooddelivery.dto.response.CartResponse;
import com.example.fooddelivery.entity.Cart;
import com.example.fooddelivery.entity.CartItem;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
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
import com.example.fooddelivery.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private CartMapper cartMapper = new CartMapper();

    @InjectMocks
    private CartServiceImpl cartService;

    private User customer1;
    private User customer2;
    private User owner;
    private CustomUserDetails customer1Details;
    private CustomUserDetails customer2Details;
    private CustomUserDetails ownerDetails;

    private Restaurant restaurant1;
    private Restaurant restaurant2;
    private MenuItem menuItem1;
    private MenuItem menuItem2;
    private MenuItem menuItemUnavailable;
    private Cart cart1;

    @BeforeEach
    void setUp() {
        customer1 = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@cart.com")
                .role(Role.CUSTOMER)
                .build();
        customer1Details = new CustomUserDetails(customer1);

        customer2 = User.builder()
                .id(2L)
                .name("Bob")
                .email("bob@cart.com")
                .role(Role.CUSTOMER)
                .build();
        customer2Details = new CustomUserDetails(customer2);

        owner = User.builder()
                .id(3L)
                .name("Mario")
                .email("mario@cart.com")
                .role(Role.RESTAURANT_OWNER)
                .build();
        ownerDetails = new CustomUserDetails(owner);

        restaurant1 = Restaurant.builder()
                .id(10L)
                .name("Mario's Pizzeria")
                .isOpen(true)
                .owner(owner)
                .build();

        restaurant2 = Restaurant.builder()
                .id(20L)
                .name("Luigi's Burgers")
                .isOpen(true)
                .owner(owner)
                .build();

        menuItem1 = MenuItem.builder()
                .id(100L)
                .name("Margherita Pizza")
                .price(new BigDecimal("299.00"))
                .isAvailable(true)
                .restaurant(restaurant1)
                .build();

        menuItem2 = MenuItem.builder()
                .id(200L)
                .name("Cheese Burger")
                .price(new BigDecimal("199.00"))
                .isAvailable(true)
                .restaurant(restaurant2)
                .build();

        menuItemUnavailable = MenuItem.builder()
                .id(300L)
                .name("Truffle Pizza")
                .price(new BigDecimal("599.00"))
                .isAvailable(false)
                .restaurant(restaurant1)
                .build();

        cart1 = Cart.builder()
                .id(50L)
                .customer(customer1)
                .restaurant(restaurant1)
                .items(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should initialize and return empty cart if customer has no existing cart")
    void getCart_ShouldCreateCart_WhenNoneExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer1));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart c = invocation.getArgument(0);
            c.setId(50L);
            return c;
        });

        CartResponse response = cartService.getCart(customer1Details);

        assertNotNull(response);
        assertEquals(50L, response.id());
        assertNull(response.restaurantId());
        assertEquals(0, response.totalItemCount());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-customer role attempts cart operation")
    void getCart_ShouldThrowAccessDenied_WhenOwnerRole() {
        assertThrows(AccessDeniedException.class, () -> cartService.getCart(ownerDetails));
    }

    @Test
    @DisplayName("Should add new item to cart and calculate correct totals")
    void addItemToCart_ShouldAddItem_WhenCartEmpty() {
        AddToCartRequest request = new AddToCartRequest(100L, 2);

        Cart emptyCart = Cart.builder()
                .id(50L)
                .customer(customer1)
                .restaurant(null)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer1));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItemToCart(request, customer1Details);

        assertNotNull(response);
        assertEquals(10L, response.restaurantId());
        assertEquals("Mario's Pizzeria", response.restaurantName());
        assertEquals(2, response.totalItemCount());
        assertEquals(new BigDecimal("598.00"), response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals("Margherita Pizza", response.items().get(0).menuItemName());
    }

    @Test
    @DisplayName("Should increment quantity when adding an item that already exists in cart")
    void addItemToCart_ShouldIncrementQuantity_WhenItemAlreadyInCart() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart1)
                .menuItem(menuItem1)
                .quantity(2)
                .price(menuItem1.getPrice())
                .build();
        cart1.addItem(existingItem);

        AddToCartRequest request = new AddToCartRequest(100L, 3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer1));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(menuItem1));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.of(cart1));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItemToCart(request, customer1Details);

        assertNotNull(response);
        assertEquals(5, response.totalItemCount());
        assertEquals(new BigDecimal("1495.00"), response.totalAmount());
    }

    @Test
    @DisplayName("Should throw CartConflictException (409) when adding item from a different restaurant")
    void addItemToCart_ShouldThrowCartConflict_WhenAddingItemFromDifferentRestaurant() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart1)
                .menuItem(menuItem1) // Belongs to Restaurant 1
                .quantity(1)
                .price(menuItem1.getPrice())
                .build();
        cart1.addItem(existingItem);

        AddToCartRequest request = new AddToCartRequest(200L, 1); // Belongs to Restaurant 2

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer1));
        when(menuItemRepository.findById(200L)).thenReturn(Optional.of(menuItem2));
        when(cartRepository.findWithDetailsByCustomerId(1L)).thenReturn(Optional.of(cart1));

        CartConflictException ex = assertThrows(CartConflictException.class, () ->
                cartService.addItemToCart(request, customer1Details)
        );
        assertTrue(ex.getMessage().contains("Cart already contains items from another restaurant"));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding an unavailable item")
    void addItemToCart_ShouldThrowException_WhenItemUnavailable() {
        AddToCartRequest request = new AddToCartRequest(300L, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer1));
        when(menuItemRepository.findById(300L)).thenReturn(Optional.of(menuItemUnavailable));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartService.addItemToCart(request, customer1Details)
        );
        assertTrue(ex.getMessage().contains("currently unavailable"));
    }

    @Test
    @DisplayName("Should update quantity of cart item and recalculate subtotal")
    void updateCartItemQuantity_ShouldUpdate_WhenValid() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart1)
                .menuItem(menuItem1)
                .quantity(2)
                .price(menuItem1.getPrice())
                .build();
        cart1.addItem(existingItem);

        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(existingItem));

        CartResponse response = cartService.updateCartItemQuantity(1L, request, customer1Details);

        assertNotNull(response);
        assertEquals(5, response.totalItemCount());
        assertEquals(new BigDecimal("1495.00"), response.totalAmount());
        assertEquals(5, existingItem.getQuantity());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when attempting to update another customer's cart item")
    void updateCartItemQuantity_ShouldThrowAccessDenied_WhenDifferentCustomer() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart1) // Belongs to customer1
                .menuItem(menuItem1)
                .quantity(2)
                .price(menuItem1.getPrice())
                .build();

        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(existingItem));

        assertThrows(AccessDeniedException.class, () ->
                cartService.updateCartItemQuantity(1L, request, customer2Details)
        );
    }

    @Test
    @DisplayName("Should remove item and reset restaurant to null when last item is removed")
    void removeCartItem_ShouldResetRestaurant_WhenCartBecomesEmpty() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart1)
                .menuItem(menuItem1)
                .quantity(1)
                .price(menuItem1.getPrice())
                .build();
        cart1.addItem(existingItem);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(existingItem));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeCartItem(1L, customer1Details);

        assertNotNull(response);
        assertEquals(0, response.totalItemCount());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
        assertNull(response.restaurantId());
        verify(cartItemRepository).delete(existingItem);
    }

    @Test
    @DisplayName("Should clear all items and reset restaurant on clearCart")
    void clearCart_ShouldClearAllItems() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart1)
                .menuItem(menuItem1)
                .quantity(1)
                .price(menuItem1.getPrice())
                .build();
        cart1.addItem(existingItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer1));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart1));

        cartService.clearCart(customer1Details);

        assertTrue(cart1.getItems().isEmpty());
        assertNull(cart1.getRestaurant());
        verify(cartRepository).save(cart1);
    }
}
