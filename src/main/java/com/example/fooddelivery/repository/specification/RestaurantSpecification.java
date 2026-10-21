package com.example.fooddelivery.repository.specification;

import com.example.fooddelivery.entity.Restaurant;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification builder providing explicit Boolean criteria composition for restaurant searches.
 */
public final class RestaurantSpecification {

    private RestaurantSpecification() {
        // Prevent instantiation
    }

    /**
     * Composes query predicates guaranteeing exact Boolean precedence:
     * {@code ((LOWER(name) LIKE :pattern OR (description IS NOT NULL AND LOWER(description) LIKE :pattern)) AND isOpen = :isOpen)}
     *
     * @param search optional text to match against name or description
     * @param isOpen optional operational status filter
     * @return JPA Specification predicate
     */
    public static Specification<Restaurant> withFilters(String search, Boolean isOpen) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Text search matching name OR description
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

            // 2. Boolean isOpen filter
            if (isOpen != null) {
                predicates.add(cb.equal(root.get("isOpen"), isOpen));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
