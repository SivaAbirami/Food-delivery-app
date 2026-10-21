package com.example.fooddelivery.service.impl;

import com.example.fooddelivery.config.CacheNames;
import com.example.fooddelivery.dto.request.RestaurantRequest;
import com.example.fooddelivery.dto.request.RestaurantStatusRequest;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.dto.response.RestaurantResponse;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.RestaurantMapper;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.repository.specification.RestaurantSpecification;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.RestaurantService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of RestaurantService.
 */
@Service
public class RestaurantServiceImpl implements RestaurantService {

    private static final int MAX_PAGE_SIZE = 50;

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantMapper restaurantMapper;

    public RestaurantServiceImpl(
            RestaurantRepository restaurantRepository,
            UserRepository userRepository,
            RestaurantMapper restaurantMapper
    ) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.restaurantMapper = restaurantMapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.RESTAURANTS, allEntries = true)
    public RestaurantResponse createRestaurant(RestaurantRequest request, CustomUserDetails currentUser) {
        validateOwnerRole(currentUser);

        // Load managed owner entity
        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner account not found"));

        Restaurant restaurant = Restaurant.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .address(request.address().trim())
                .phone(request.phone().trim())
                .owner(owner)
                .isOpen(true) // Default to open upon creation
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return restaurantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.RESTAURANT_DETAILS, key = "#id")
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = findRestaurantById(id);
        return restaurantMapper.toResponse(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.RESTAURANTS, key = "T(com.example.fooddelivery.config.CacheKeyUtils).restaurantListKey(#search, #isOpen, #pageable)")
    public PagedResponse<RestaurantResponse> getAllRestaurants(String search, Boolean isOpen, Pageable pageable) {
        Pageable safePageable = createSafePageable(pageable);
        Specification<Restaurant> spec = RestaurantSpecification.withFilters(search, isOpen);
        Page<Restaurant> page = restaurantRepository.findAll(spec, safePageable);

        return PagedResponse.from(page.map(restaurantMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RestaurantResponse> getMyRestaurants(CustomUserDetails currentUser, Pageable pageable) {
        validateOwnerRole(currentUser);
        Pageable safePageable = createSafePageable(pageable);
        Page<Restaurant> page = restaurantRepository.findByOwnerId(currentUser.getId(), safePageable);

        return PagedResponse.from(page.map(restaurantMapper::toResponse));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_DETAILS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.RESTAURANTS, allEntries = true)
    })
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request, CustomUserDetails currentUser) {
        Restaurant restaurant = findRestaurantById(id);
        verifyOwnership(restaurant, currentUser);

        restaurant.setName(request.name().trim());
        restaurant.setDescription(request.description() != null ? request.description().trim() : null);
        restaurant.setAddress(request.address().trim());
        restaurant.setPhone(request.phone().trim());

        Restaurant updated = restaurantRepository.save(restaurant);
        return restaurantMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_DETAILS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.RESTAURANTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_MENUS, allEntries = true)
    })
    public RestaurantResponse toggleRestaurantStatus(Long id, RestaurantStatusRequest request, CustomUserDetails currentUser) {
        Restaurant restaurant = findRestaurantById(id);
        verifyOwnership(restaurant, currentUser);

        restaurant.setOpen(request.isOpen());
        Restaurant updated = restaurantRepository.save(restaurant);
        return restaurantMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_DETAILS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.RESTAURANTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_MENUS, allEntries = true)
    })
    public void deleteRestaurant(Long id, CustomUserDetails currentUser) {
        Restaurant restaurant = findRestaurantById(id);
        verifyOwnership(restaurant, currentUser);

        restaurantRepository.delete(restaurant);
    }

    private Restaurant findRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }

    /**
     * Enforces that the authenticated user possesses the RESTAURANT_OWNER role.
     */
    private void validateOwnerRole(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (!Role.RESTAURANT_OWNER.equals(currentUser.getUser().getRole())) {
            throw new AccessDeniedException("User must have RESTAURANT_OWNER role to perform this operation");
        }
    }

    /**
     * Enforces resource ownership: the authenticated user must be the exact owner of the restaurant.
     */
    private void verifyOwnership(Restaurant restaurant, CustomUserDetails currentUser) {
        validateOwnerRole(currentUser);
        if (!restaurant.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to modify or delete this restaurant");
        }
    }

    /**
     * Clamps page size to a safe maximum (50) to prevent database Denial of Service / memory exhaustion.
     */
    private Pageable createSafePageable(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return PageRequest.of(0, 10);
        }
        int clampedSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), clampedSize, pageable.getSort());
    }
}
