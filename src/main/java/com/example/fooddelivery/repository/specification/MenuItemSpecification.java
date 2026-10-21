package com.example.fooddelivery.repository.specification;

import com.example.fooddelivery.entity.MenuItem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification builder providing explicit criteria composition for restaurant menu queries.
 */
public final class MenuItemSpecification {

    private MenuItemSpecification() {
        // Prevent instantiation
    }

    /**
     * Composes query predicates guaranteeing exact Boolean grouping:
     * {@code restaurant.id = :restaurantId AND [category = :category] AND [isAvailable = :available] AND [(LOWER(name) LIKE :pattern OR (description IS NOT NULL AND LOWER(description) LIKE :pattern))]}
     *
     * @param restaurantId mandatory restaurant scope ID
     * @param category optional category filter (case-insensitive)
     * @param available optional availability flag
     * @param search optional search keyword matching name or description
     * @return JPA Specification predicate
     */
    public static Specification<MenuItem> withFilters(Long restaurantId, String category, Boolean available, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory restaurant scope
            predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));

            // 2. Optional Category filter (case-insensitive)
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            // 3. Optional Availability filter
            if (available != null) {
                predicates.add(cb.equal(root.get("isAvailable"), available));
            }

            // 4. Optional Text search matching name OR description
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.and(
                        cb.isNotNull(root.get("description")),
                        cb.like(cb.lower(root.get("description")), pattern)
                );
                // Explicit OR grouping
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
