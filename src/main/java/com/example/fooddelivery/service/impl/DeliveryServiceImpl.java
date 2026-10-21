package com.example.fooddelivery.service.impl;

import com.example.fooddelivery.dto.response.DeliveryStatusResponse;
import com.example.fooddelivery.dto.response.DeliverySummaryResponse;
import com.example.fooddelivery.dto.response.PagedResponse;
import com.example.fooddelivery.entity.Order;
import com.example.fooddelivery.entity.User;
import com.example.fooddelivery.enums.OrderStatus;
import com.example.fooddelivery.enums.OrderStatusTransitionValidator;
import com.example.fooddelivery.enums.Role;
import com.example.fooddelivery.exception.IllegalOrderStateTransitionException;
import com.example.fooddelivery.exception.OrderConflictException;
import com.example.fooddelivery.exception.ResourceNotFoundException;
import com.example.fooddelivery.mapper.DeliveryMapper;
import com.example.fooddelivery.repository.OrderRepository;
import com.example.fooddelivery.repository.UserRepository;
import com.example.fooddelivery.security.CustomUserDetails;
import com.example.fooddelivery.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Production implementation of DeliveryService.
 * Enforces strict role verification, assigned driver ownership, and concurrency-safe atomic order claiming.
 */
@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryMapper deliveryMapper;

    public DeliveryServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            DeliveryMapper deliveryMapper
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DeliverySummaryResponse> getAvailableDeliveries(
            Pageable pageable,
            CustomUserDetails currentUser
    ) {
        validateDeliveryPartnerRole(currentUser);
        Pageable safePageable = createSafePageable(pageable);
        Page<Order> orders = orderRepository.findByStatusAndDeliveryPartnerIsNull(OrderStatus.READY_FOR_PICKUP, safePageable);
        return PagedResponse.from(orders.map(deliveryMapper::toSummaryResponse));
    }

    @Override
    @Transactional
    public DeliverySummaryResponse claimDelivery(Long orderId, CustomUserDetails currentUser) {
        validateDeliveryPartnerRole(currentUser);

        User partner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with ID: " + currentUser.getId()));

        // Concurrency-safe atomic database update
        int rowsUpdated = orderRepository.claimOrder(orderId, OrderStatus.READY_FOR_PICKUP, partner, Instant.now());

        if (rowsUpdated == 0) {
            Order existingOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

            if (existingOrder.getStatus() != OrderStatus.READY_FOR_PICKUP) {
                throw new IllegalOrderStateTransitionException(
                        String.format("Order is in status '%s' and is not available for pickup", existingOrder.getStatus())
                );
            }

            if (existingOrder.getDeliveryPartner() != null) {
                throw new OrderConflictException("Order has already been claimed by another delivery partner");
            }

            throw new OrderConflictException("Could not claim order due to concurrent modification");
        }

        Order claimedOrder = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        log.info("Order ID: {} successfully claimed by delivery partner ID: {}", orderId, partner.getId());
        return deliveryMapper.toSummaryResponse(claimedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DeliverySummaryResponse> getMyDeliveries(
            Pageable pageable,
            CustomUserDetails currentUser
    ) {
        validateDeliveryPartnerRole(currentUser);
        Pageable safePageable = createSafePageable(pageable);
        Page<Order> orders = orderRepository.findByDeliveryPartnerId(currentUser.getId(), safePageable);
        return PagedResponse.from(orders.map(deliveryMapper::toSummaryResponse));
    }

    @Override
    @Transactional
    public DeliveryStatusResponse pickupDelivery(Long orderId, CustomUserDetails currentUser) {
        validateDeliveryPartnerRole(currentUser);

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        verifyAssignedDriverOwnership(order, currentUser);

        OrderStatusTransitionValidator.validateDeliveryPartnerTransition(order.getStatus(), OrderStatus.OUT_FOR_DELIVERY);

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order = orderRepository.save(order);

        log.info("Order ID: {} status transitioned to OUT_FOR_DELIVERY by delivery partner ID: {}", orderId, currentUser.getId());
        return deliveryMapper.toStatusResponse(order);
    }

    @Override
    @Transactional
    public DeliveryStatusResponse completeDelivery(Long orderId, CustomUserDetails currentUser) {
        validateDeliveryPartnerRole(currentUser);

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        verifyAssignedDriverOwnership(order, currentUser);

        OrderStatusTransitionValidator.validateDeliveryPartnerTransition(order.getStatus(), OrderStatus.DELIVERED);

        order.setStatus(OrderStatus.DELIVERED);
        order = orderRepository.save(order);

        log.info("Order ID: {} status transitioned to DELIVERED by delivery partner ID: {}", orderId, currentUser.getId());
        return deliveryMapper.toStatusResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryStatusResponse getCustomerOrderDeliveryStatus(Long orderId, CustomUserDetails currentUser) {
        validateCustomerRole(currentUser);

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view delivery status for this order");
        }

        return deliveryMapper.toStatusResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryStatusResponse getOwnerOrderDeliveryStatus(Long orderId, CustomUserDetails currentUser) {
        validateOwnerRole(currentUser);

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view delivery status for this restaurant's order");
        }

        return deliveryMapper.toStatusResponse(order);
    }

    private void validateDeliveryPartnerRole(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (!Role.DELIVERY_PARTNER.equals(currentUser.getUser().getRole())) {
            throw new AccessDeniedException("User must have DELIVERY_PARTNER role to perform this operation");
        }
    }

    private void validateCustomerRole(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (!Role.CUSTOMER.equals(currentUser.getUser().getRole())) {
            throw new AccessDeniedException("User must have CUSTOMER role to perform this operation");
        }
    }

    private void validateOwnerRole(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (!Role.RESTAURANT_OWNER.equals(currentUser.getUser().getRole())) {
            throw new AccessDeniedException("User must have RESTAURANT_OWNER role to perform this operation");
        }
    }

    private void verifyAssignedDriverOwnership(Order order, CustomUserDetails currentUser) {
        if (order.getDeliveryPartner() == null || !order.getDeliveryPartner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not the assigned delivery partner for this order");
        }
    }

    private Pageable createSafePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 10);
        }
        int clampedSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), clampedSize, pageable.getSort());
    }
}
