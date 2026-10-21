package com.example.fooddelivery.enums;

import com.example.fooddelivery.exception.IllegalOrderStateTransitionException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Centralized validator enforcing strict order status lifecycle state machine transitions.
 */
public final class OrderStatusTransitionValidator {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS;
    private static final Set<OrderStatus> OWNER_CANCELLABLE_STATES;

    static {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);

        transitions.put(OrderStatus.PLACED, EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
        transitions.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
        transitions.put(OrderStatus.DELIVERED, Collections.emptySet());
        transitions.put(OrderStatus.CANCELLED, Collections.emptySet());

        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
        OWNER_CANCELLABLE_STATES = Collections.unmodifiableSet(
                EnumSet.of(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING)
        );
    }

    private OrderStatusTransitionValidator() {
        // Utility class
    }

    /**
     * Validates whether a direct state transition is permitted by the state machine.
     *
     * @param from Current order status
     * @param to Target order status
     */
    public static void validateTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            throw new IllegalOrderStateTransitionException("Source and target order status must not be null");
        }

        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        if (!allowed.contains(to)) {
            throw new IllegalOrderStateTransitionException(
                    String.format("Invalid order state transition from '%s' to '%s'", from, to)
            );
        }
    }

    /**
     * Validates restaurant owner specific status update.
     */
    public static void validateOwnerTransition(OrderStatus from, OrderStatus to) {
        validateTransition(from, to);

        if (to == OrderStatus.CANCELLED && !OWNER_CANCELLABLE_STATES.contains(from)) {
            throw new IllegalOrderStateTransitionException(
                    String.format("Restaurant owner cannot cancel an order in '%s' status", from)
            );
        }

        if (to != OrderStatus.ACCEPTED && to != OrderStatus.PREPARING
                && to != OrderStatus.READY_FOR_PICKUP && to != OrderStatus.CANCELLED) {
            throw new IllegalOrderStateTransitionException(
                    String.format("Restaurant owner cannot transition order to '%s'", to)
            );
        }
    }

    /**
     * Validates customer specific status update (cancellation).
     */
    public static void validateCustomerCancellation(OrderStatus currentStatus) {
        if (currentStatus != OrderStatus.PLACED) {
            throw new IllegalOrderStateTransitionException(
                    String.format("Customer can only cancel orders in 'PLACED' status. Current status is '%s'", currentStatus)
            );
        }
    }

    /**
     * Validates delivery partner specific status update.
     */
    public static void validateDeliveryPartnerTransition(OrderStatus from, OrderStatus to) {
        validateTransition(from, to);

        if (from == OrderStatus.READY_FOR_PICKUP && to != OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalOrderStateTransitionException(
                    String.format("Delivery partner can only transition from 'READY_FOR_PICKUP' to 'OUT_FOR_DELIVERY', not '%s'", to)
            );
        }

        if (from == OrderStatus.OUT_FOR_DELIVERY && to != OrderStatus.DELIVERED) {
            throw new IllegalOrderStateTransitionException(
                    String.format("Delivery partner can only transition from 'OUT_FOR_DELIVERY' to 'DELIVERED', not '%s'", to)
            );
        }
    }
}
