package com.example.fooddelivery.exception;

/**
 * Exception thrown when adding an item from a different restaurant to an already populated cart.
 */
public class CartConflictException extends RuntimeException {
    public CartConflictException(String message) {
        super(message);
    }
}
