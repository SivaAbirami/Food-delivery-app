package com.example.fooddelivery.controller;

import com.example.fooddelivery.dto.request.RestaurantRequest;
import com.example.fooddelivery.dto.request.RestaurantStatusRequest;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.dto.response.RestaurantResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.RestaurantService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for restaurant browsing, searching, and owner management operations.
 */
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    /**
     * Public endpoint to browse and search restaurants with pagination and filtering.
     *
     * @param search Optional text to match against name or description
     * @param isOpen Optional boolean status filter (e.g. true for open only)
     * @param pageable Pagination parameters (defaults to page 0, size 10, sort by createdAt desc)
     * @return 200 OK with PagedResponse
     */
    @GetMapping
    public ResponseEntity<PagedResponse<RestaurantResponse>> getAllRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isOpen,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<RestaurantResponse> response = restaurantService.getAllRestaurants(search, isOpen, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Public endpoint to view a specific restaurant's profile.
     *
     * @param id Restaurant ID
     * @return 200 OK with RestaurantResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse response = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint for authenticated restaurant owners to list their own restaurants.
     *
     * @param currentUser Authenticated owner principal
     * @param pageable Pagination parameters
     * @return 200 OK with PagedResponse
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<PagedResponse<RestaurantResponse>> getMyRestaurants(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<RestaurantResponse> response = restaurantService.getMyRestaurants(currentUser, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new restaurant outlet owned by the authenticated owner.
     *
     * @param request Validated restaurant details
     * @param currentUser Authenticated principal
     * @return 201 CREATED with RestaurantResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        RestaurantResponse response = restaurantService.createRestaurant(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing restaurant outlet. Requires authenticated user to be the true owner.
     *
     * @param id Restaurant ID
     * @param request Validated update details
     * @param currentUser Authenticated principal
     * @return 200 OK with updated RestaurantResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        RestaurantResponse response = restaurantService.updateRestaurant(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles restaurant open/closed status. Requires authenticated user to be the true owner.
     *
     * @param id Restaurant ID
     * @param request Status toggle payload
     * @param currentUser Authenticated principal
     * @return 200 OK with updated RestaurantResponse
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> toggleRestaurantStatus(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        RestaurantResponse response = restaurantService.toggleRestaurantStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a restaurant outlet. Requires authenticated user to be the true owner.
     *
     * @param id Restaurant ID
     * @param currentUser Authenticated principal
     * @return 204 NO_CONTENT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<Void> deleteRestaurant(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        restaurantService.deleteRestaurant(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
