package com.example.fooddelivery.repository;

import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"customer", "restaurant", "deliveryPartner", "items"})
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "restaurant", "deliveryPartner", "items"})
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    @EntityGraph(attributePaths = {"customer", "restaurant", "deliveryPartner", "items"})
    Page<Order> findByRestaurantId(Long restaurantId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "restaurant", "deliveryPartner", "items"})
    Page<Order> findByRestaurantIdAndStatus(Long restaurantId, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "restaurant", "deliveryPartner", "items"})
    Page<Order> findByDeliveryPartnerId(Long deliveryPartnerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "restaurant", "deliveryPartner", "items"})
    Page<Order> findByStatusAndDeliveryPartnerIsNull(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "restaurant", "deliveryPartner", "items", "items.menuItem"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findWithDetailsById(@Param("id") Long id);

    /**
     * Atomically claims an order for a delivery partner if and only if the order
     * is currently in the specified status (READY_FOR_PICKUP) and has no delivery partner assigned yet.
     *
     * @return Number of rows updated (1 if successfully claimed, 0 if already claimed or status mismatch)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o SET o.deliveryPartner = :partner, o.updatedAt = :now WHERE o.id = :orderId AND o.status = :status AND o.deliveryPartner IS NULL")
    int claimOrder(
            @Param("orderId") Long orderId,
            @Param("status") OrderStatus status,
            @Param("partner") User partner,
            @Param("now") Instant now
    );
}
