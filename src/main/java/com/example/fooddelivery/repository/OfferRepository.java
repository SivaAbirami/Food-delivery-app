package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByIsActiveTrue();

    @Query("SELECT o FROM Offer o WHERE o.isActive = true AND (o.restaurant IS NULL OR o.restaurant.id = :restaurantId)")
    List<Offer> findActiveOffersForRestaurant(@Param("restaurantId") Long restaurantId);

    Optional<Offer> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    boolean existsByCodeIgnoreCase(String code);
}

