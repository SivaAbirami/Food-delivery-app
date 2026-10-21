package com.example.fooddelivery.service.impl;

import com.example.fooddelivery.config.CacheNames;
import com.example.fooddelivery.dto.request.MenuItemRequest;
import com.example.fooddelivery.dto.request.MenuItemStatusRequest;
import com.example.fooddelivery.dto.response.MenuItemResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.entity.MenuItem;
import com.example.fooddelivery.entity.Restaurant;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.MenuItemMapper;
import com.example.fooddelivery.repository.MenuItemRepository;
import com.example.fooddelivery.repository.RestaurantRepository;
import com.example.fooddelivery.repository.specification.MenuItemSpecification;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.MenuItemService;
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
 * Production implementation of MenuItemService.
 */
@Service
public class MenuItemServiceImpl implements MenuItemService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemMapper menuItemMapper;

    public MenuItemServiceImpl(
            MenuItemRepository menuItemRepository,
            RestaurantRepository restaurantRepository,
            MenuItemMapper menuItemMapper
    ) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemMapper = menuItemMapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.RESTAURANT_MENUS, allEntries = true)
    public MenuItemResponse createMenuItem(Long restaurantId, MenuItemRequest request, CustomUserDetails currentUser) {
        validateOwnerRole(currentUser);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        verifyRestaurantOwnership(restaurant, currentUser);

        MenuItem menuItem = menuItemMapper.toEntity(request, restaurant);
        MenuItem saved = menuItemRepository.save(menuItem);

        return menuItemMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.MENU_ITEMS, key = "#id")
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem menuItem = findMenuItemById(id);
        return menuItemMapper.toResponse(menuItem);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.RESTAURANT_MENUS, key = "T(com.example.fooddelivery.config.CacheKeyUtils).menuListKey(#restaurantId, #category, #available, #search, #pageable)")
    public PagedResponse<MenuItemResponse> getMenuItemsByRestaurant(
            Long restaurantId,
            String category,
            Boolean available,
            String search,
            Pageable pageable
    ) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + restaurantId);
        }

        Pageable safePageable = createSafePageable(pageable);
        Specification<MenuItem> spec = MenuItemSpecification.withFilters(restaurantId, category, available, search);
        Page<MenuItem> page = menuItemRepository.findAll(spec, safePageable);

        return PagedResponse.from(page.map(menuItemMapper::toResponse));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.MENU_ITEMS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_MENUS, allEntries = true)
    })
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request, CustomUserDetails currentUser) {
        MenuItem menuItem = findMenuItemById(id);
        verifyTransitiveOwnership(menuItem, currentUser);

        menuItem.setName(request.name().trim());
        menuItem.setDescription(request.description() != null ? request.description().trim() : null);
        menuItem.setPrice(request.price());
        menuItem.setCategory(request.category().trim());
        menuItem.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);

        MenuItem updated = menuItemRepository.save(menuItem);
        return menuItemMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.MENU_ITEMS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_MENUS, allEntries = true)
    })
    public MenuItemResponse toggleAvailability(Long id, MenuItemStatusRequest request, CustomUserDetails currentUser) {
        MenuItem menuItem = findMenuItemById(id);
        verifyTransitiveOwnership(menuItem, currentUser);

        menuItem.setAvailable(request.isAvailable());
        MenuItem updated = menuItemRepository.save(menuItem);
        return menuItemMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.MENU_ITEMS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.RESTAURANT_MENUS, allEntries = true)
    })
    public void deleteMenuItem(Long id, CustomUserDetails currentUser) {
        MenuItem menuItem = findMenuItemById(id);
        verifyTransitiveOwnership(menuItem, currentUser);

        menuItemRepository.delete(menuItem);
    }

    private MenuItem findMenuItemById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
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
     * Enforces that the authenticated user owns the parent restaurant.
     */
    private void verifyRestaurantOwnership(Restaurant restaurant, CustomUserDetails currentUser) {
        validateOwnerRole(currentUser);
        if (!restaurant.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to manage menu items for this restaurant");
        }
    }

    /**
     * Enforces transitive resource ownership: verifies that the authenticated user owns the restaurant
     * to which this menu item belongs.
     */
    private void verifyTransitiveOwnership(MenuItem menuItem, CustomUserDetails currentUser) {
        validateOwnerRole(currentUser);
        if (menuItem.getRestaurant() == null || menuItem.getRestaurant().getOwner() == null
                || !menuItem.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to modify or delete this menu item");
        }
    }

    /**
     * Clamps page size to a safe maximum (50) to prevent Denial of Service / excessive memory consumption.
     */
    private Pageable createSafePageable(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return PageRequest.of(0, 10);
        }
        int clampedSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), clampedSize, pageable.getSort());
    }
}
