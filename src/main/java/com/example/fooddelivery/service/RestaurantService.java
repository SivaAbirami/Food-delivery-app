package com.example.fooddelivery.service;

import com.example.fooddelivery.dto.request.RestaurantRequest;
import com.example.fooddelivery.dto.request.RestaurantStatusRequest;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.dto.response.RestaurantResponse;
import com.example.fooddelivery.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for restaurant management, searching, status toggling, and resource ownership authorization.
 */
public interface RestaurantService {

    /**
     * Creates a new restaurant outlet owned by the authenticated RESTAURANT_OWNER.
     *
     * @param request Validated restaurant details
     * @param currentUser Authenticated owner principal
     * @return RestaurantResponse
     */
    RestaurantResponse createRestaurant(RestaurantRequest request, CustomUserDetails currentUser);

    /**
     * Retrieves public restaurant profile by ID.
     *
     * @param id Restaurant primary key
     * @return RestaurantResponse
     */
    RestaurantResponse getRestaurantById(Long id);

    /**
     * Browses and searches restaurants with pagination and optional filters.
     *
     * @param search Optional text to match against name or description
     * @param isOpen Optional boolean status filter
     * @param pageable Pagination and sorting parameters
     * @return Paginated list of restaurant responses
     */
    PagedResponse<RestaurantResponse> getAllRestaurants(String search, Boolean isOpen, Pageable pageable);

    /**
     * Lists all restaurants owned by the authenticated owner.
     *
     * @param currentUser Authenticated owner principal
     * @param pageable Pagination parameters
     * @return Paginated list of owned restaurant responses
     */
    PagedResponse<RestaurantResponse> getMyRestaurants(CustomUserDetails currentUser, Pageable pageable);

    /**
     * Updates restaurant metadata. Requires authenticated user to be the true owner.
     *
     * @param id Restaurant ID
     * @param request Updated restaurant details
     * @param currentUser Authenticated principal
     * @return Updated RestaurantResponse
     */
    RestaurantResponse updateRestaurant(Long id, RestaurantRequest request, CustomUserDetails currentUser);

    /**
     * Toggles restaurant open/closed status. Requires authenticated user to be the true owner.
     *
     * @param id Restaurant ID
     * @param request Status update payload
     * @param currentUser Authenticated principal
     * @return Updated RestaurantResponse
     */
    RestaurantResponse toggleRestaurantStatus(Long id, RestaurantStatusRequest request, CustomUserDetails currentUser);

    /**
     * Deletes a restaurant outlet. Requires authenticated user to be the true owner.
     *
     * @param id Restaurant ID
     * @param currentUser Authenticated principal
     */
    void deleteRestaurant(Long id, CustomUserDetails currentUser);
}
