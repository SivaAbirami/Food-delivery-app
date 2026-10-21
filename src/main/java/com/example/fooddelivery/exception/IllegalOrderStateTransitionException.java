package com.example.fooddelivery.exception;

/**
 * Exception thrown when an illegal or unsupported order status lifecycle transition is attempted.
 */
public class IllegalOrderStateTransitionException extends RuntimeException {
    public IllegalOrderStateTransitionException(String message) {
        super(message);
    }
}
