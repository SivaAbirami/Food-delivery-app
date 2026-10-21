package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for MenuItem entity operations.
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long>, JpaSpecificationExecutor<MenuItem> {

    @EntityGraph(attributePaths = {"restaurant"})
    @NonNull
    Page<MenuItem> findAll(@NonNull Specification<MenuItem> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"restaurant"})
    @NonNull
    Page<MenuItem> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"restaurant", "restaurant.owner"})
    @NonNull
    Optional<MenuItem> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"restaurant"})
    Page<MenuItem> findByRestaurantId(Long restaurantId, Pageable pageable);
}
