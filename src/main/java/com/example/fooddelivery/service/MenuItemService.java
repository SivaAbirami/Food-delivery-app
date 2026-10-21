package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.MenuItemRequest;
import com.example.fooddelivery.dto.request.MenuItemStatusRequest;
import com.example.fooddelivery.dto.response.MenuItemResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for menu item management, browsing, searching, and status operations.
 */
public interface MenuItemService {

    /**
     * Creates a new menu item for the specified restaurant.
     * Enforces that the authenticated user is the true owner of the target restaurant.
     *
     * @param restaurantId Parent restaurant ID
     * @param request Validated menu item details
     * @param currentUser Authenticated owner principal
     * @return MenuItemResponse of the newly created item
     */
    MenuItemResponse createMenuItem(Long restaurantId, MenuItemRequest request, CustomUserDetails currentUser);

    /**
     * Retrieves details for a specific menu item. Publicly accessible.
     *
     * @param id Menu item ID
     * @return MenuItemResponse DTO
     */
    MenuItemResponse getMenuItemById(Long id);

    /**
     * Lists and filters menu items for a specific restaurant with pagination. Publicly accessible.
     *
     * @param restaurantId Parent restaurant ID
     * @param category Optional category filter
     * @param available Optional availability filter
     * @param search Optional keyword search in name or description
     * @param pageable Pagination parameters
     * @return PagedResponse of MenuItemResponse
     */
    PagedResponse<MenuItemResponse> getMenuItemsByRestaurant(
            Long restaurantId,
            String category,
            Boolean available,
            String search,
            Pageable pageable
    );

    /**
     * Updates an existing menu item.
     * Enforces transitive ownership (user must own the parent restaurant).
     *
     * @param id Menu item ID
     * @param request Validated update details
     * @param currentUser Authenticated owner principal
     * @return Updated MenuItemResponse
     */
    MenuItemResponse updateMenuItem(Long id, MenuItemRequest request, CustomUserDetails currentUser);

    /**
     * Toggles availability status of an existing menu item.
     * Enforces transitive ownership (user must own the parent restaurant).
     *
     * @param id Menu item ID
     * @param request Status toggle payload
     * @param currentUser Authenticated owner principal
     * @return Updated MenuItemResponse
     */
    MenuItemResponse toggleAvailability(Long id, MenuItemStatusRequest request, CustomUserDetails currentUser);

    /**
     * Deletes an existing menu item.
     * Enforces transitive ownership (user must own the parent restaurant).
     *
     * @param id Menu item ID
     * @param currentUser Authenticated owner principal
     */
    void deleteMenuItem(Long id, CustomUserDetails currentUser);
}
