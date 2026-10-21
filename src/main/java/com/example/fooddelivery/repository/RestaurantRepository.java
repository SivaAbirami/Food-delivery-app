package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.Restaurant;
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
 * Spring Data JPA repository for Restaurant entity operations.
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {

    @EntityGraph(attributePaths = {"owner"})
    @NonNull
    Page<Restaurant> findAll(@NonNull Specification<Restaurant> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    @NonNull
    Page<Restaurant> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    Page<Restaurant> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    Page<Restaurant> findByIsOpen(boolean isOpen, Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    @NonNull
    Optional<Restaurant> findById(@NonNull Long id);
}
