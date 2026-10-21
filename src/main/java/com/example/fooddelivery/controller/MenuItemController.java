package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.MenuItemRequest;
import com.example.fooddelivery.dto.request.MenuItemStatusRequest;
import com.example.fooddelivery.dto.response.MenuItemResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for menu browsing, searching, and restaurant owner menu management.
 */
@RestController
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    /**
     * Public endpoint to list and search a restaurant's menu items with pagination and filters.
     *
     * @param restaurantId Parent restaurant ID
     * @param category Optional category filter
     * @param available Optional availability flag
     * @param search Optional keyword search in name or description
     * @param pageable Pagination parameters
     * @return 200 OK with PagedResponse of MenuItemResponse
     */
    @GetMapping("/api/restaurants/{restaurantId}/menu")
    public ResponseEntity<PagedResponse<MenuItemResponse>> getRestaurantMenu(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<MenuItemResponse> response = menuItemService.getMenuItemsByRestaurant(
                restaurantId, category, available, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new menu item for a restaurant.
     * Requires ROLE_RESTAURANT_OWNER and ownership of the target restaurant.
     *
     * @param restaurantId Parent restaurant ID
     * @param request Validated menu item details
     * @param currentUser Authenticated principal
     * @return 201 CREATED with MenuItemResponse
     */
    @PostMapping("/api/restaurants/{restaurantId}/menu")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        MenuItemResponse response = menuItemService.createMenuItem(restaurantId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public endpoint to view a specific menu item.
     *
     * @param id Menu item ID
     * @return 200 OK with MenuItemResponse
     */
    @GetMapping("/api/menu-items/{id}")
    public ResponseEntity<MenuItemResponse> getMenuItemById(@PathVariable Long id) {
        MenuItemResponse response = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing menu item.
     * Requires ROLE_RESTAURANT_OWNER and transitive ownership of the parent restaurant.
     *
     * @param id Menu item ID
     * @param request Validated update details
     * @param currentUser Authenticated principal
     * @return 200 OK with updated MenuItemResponse
     */
    @PutMapping("/api/menu-items/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        MenuItemResponse response = menuItemService.updateMenuItem(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles availability of an existing menu item.
     * Requires ROLE_RESTAURANT_OWNER and transitive ownership of the parent restaurant.
     *
     * @param id Menu item ID
     * @param request Status toggle payload
     * @param currentUser Authenticated principal
     * @return 200 OK with updated MenuItemResponse
     */
    @PatchMapping("/api/menu-items/{id}/status")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemResponse> toggleAvailability(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        MenuItemResponse response = menuItemService.toggleAvailability(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a menu item.
     * Requires ROLE_RESTAURANT_OWNER and transitive ownership of the parent restaurant.
     *
     * @param id Menu item ID
     * @param currentUser Authenticated principal
     * @return 204 NO_CONTENT
     */
    @DeleteMapping("/api/menu-items/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        menuItemService.deleteMenuItem(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
