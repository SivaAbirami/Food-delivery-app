package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Cart operations.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"restaurant", "items", "items.menuItem"})
    Optional<Cart> findWithDetailsByCustomerId(Long customerId);

    @NonNull
    @EntityGraph(attributePaths = {"restaurant", "items", "items.menuItem"})
    Optional<Cart> findById(@NonNull Long id);
}
