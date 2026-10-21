package com.example.fooddelivery.config;

import org.springframework.data.domain.Pageable;

/**
 * Utility class providing deterministic, collision-safe, normalized cache keys for parameterized queries.
 */
public final class CacheKeyUtils {

    private CacheKeyUtils() {
        // Utility class
    }

    /**
     * Generates a deterministic normalized key for paginated restaurant listings with filters.
     */
    public static String restaurantListKey(String search, Boolean isOpen, Pageable pageable) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : "";
        String normalizedIsOpen = (isOpen != null) ? isOpen.toString() : "all";
        int page = (pageable != null) ? pageable.getPageNumber() : 0;
        int size = (pageable != null) ? pageable.getPageSize() : 10;
        String sort = (pageable != null && pageable.getSort().isSorted()) ? pageable.getSort().toString() : "default";

        return String.format("list:s=%s:o=%s:p=%d:sz=%d:sort=%s",
                normalizedSearch, normalizedIsOpen, page, size, sort);
    }

    /**
     * Generates a deterministic normalized key for paginated restaurant menu listings with filters.
     */
    public static String menuListKey(Long restaurantId, String category, Boolean available, String search, Pageable pageable) {
        String normalizedCategory = (category != null && !category.isBlank()) ? category.trim().toLowerCase() : "";
        String normalizedAvailable = (available != null) ? available.toString() : "all";
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : "";
        int page = (pageable != null) ? pageable.getPageNumber() : 0;
        int size = (pageable != null) ? pageable.getPageSize() : 10;
        String sort = (pageable != null && pageable.getSort().isSorted()) ? pageable.getSort().toString() : "default";

        return String.format("menu:r=%d:c=%s:a=%s:s=%s:p=%d:sz=%d:sort=%s",
                restaurantId, normalizedCategory, normalizedAvailable, normalizedSearch, page, size, sort);
    }
}
