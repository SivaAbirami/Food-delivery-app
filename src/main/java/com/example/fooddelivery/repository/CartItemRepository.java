package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for CartItem entity operations.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {"cart", "cart.customer", "menuItem", "menuItem.restaurant"})
    @NonNull
    Optional<CartItem> findById(@NonNull Long id);

    Optional<CartItem> findByCartIdAndMenuItemId(Long cartId, Long menuItemId);
}
