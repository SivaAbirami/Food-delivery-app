package com.example.fooddelivery.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheKeyUtils Unit Tests")
class CacheKeyUtilsTest {

    @Test
    @DisplayName("restaurantListKey: Normalizes search parameter and trims/lowercases")
    void testRestaurantListKeyNormalization() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());

        String key1 = CacheKeyUtils.restaurantListKey("  Pizza  ", true, pageable);
        String key2 = CacheKeyUtils.restaurantListKey("pizza", true, pageable);
        String key3 = CacheKeyUtils.restaurantListKey("PIZZA", true, pageable);

        assertThat(key1).isEqualTo(key2).isEqualTo(key3);
        assertThat(key1).contains("s=pizza");
    }

    @Test
    @DisplayName("restaurantListKey: Different pagination and sort produce distinct keys")
    void testRestaurantListKeyPaginationAndSortSeparation() {
        Pageable page0 = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Pageable page1 = PageRequest.of(1, 10, Sort.by("createdAt").descending());
        Pageable page0NameAsc = PageRequest.of(0, 10, Sort.by("name").ascending());

        String keyPage0 = CacheKeyUtils.restaurantListKey(null, null, page0);
        String keyPage1 = CacheKeyUtils.restaurantListKey(null, null, page1);
        String keyPage0Name = CacheKeyUtils.restaurantListKey(null, null, page0NameAsc);

        assertThat(keyPage0).isNotEqualTo(keyPage1);
        assertThat(keyPage0).isNotEqualTo(keyPage0Name);
    }

    @Test
    @DisplayName("restaurantListKey: Filter isOpen handles null vs true vs false")
    void testRestaurantListKeyIsOpenFilter() {
        Pageable pageable = PageRequest.of(0, 10);

        String keyAll = CacheKeyUtils.restaurantListKey(null, null, pageable);
        String keyOpen = CacheKeyUtils.restaurantListKey(null, true, pageable);
        String keyClosed = CacheKeyUtils.restaurantListKey(null, false, pageable);

        assertThat(keyAll).contains("o=all");
        assertThat(keyOpen).contains("o=true");
        assertThat(keyClosed).contains("o=false");
        assertThat(keyOpen).isNotEqualTo(keyClosed);
        assertThat(keyOpen).isNotEqualTo(keyAll);
    }

    @Test
    @DisplayName("menuListKey: Normalizes parameters and prevents collisions")
    void testMenuListKeyNormalizationAndCollisions() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());

        String key1 = CacheKeyUtils.menuListKey(1L, "  Drinks ", true, " COLA ", pageable);
        String key2 = CacheKeyUtils.menuListKey(1L, "drinks", true, "cola", pageable);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1).contains("r=1:c=drinks:a=true:s=cola");

        String keyDifferentRest = CacheKeyUtils.menuListKey(2L, "drinks", true, "cola", pageable);
        assertThat(key1).isNotEqualTo(keyDifferentRest);

        String keyDifferentCategory = CacheKeyUtils.menuListKey(1L, "desserts", true, "cola", pageable);
        assertThat(key1).isNotEqualTo(keyDifferentCategory);

        String keyDifferentAvailability = CacheKeyUtils.menuListKey(1L, "drinks", false, "cola", pageable);
        assertThat(key1).isNotEqualTo(keyDifferentAvailability);
    }
}
