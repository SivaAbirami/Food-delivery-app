package com.example.fooddelivery.exception;

/**
 * Exception thrown when a conflict or race condition occurs on an order (e.g. concurrent delivery claiming).
 */
public class OrderConflictException extends RuntimeException {
    public OrderConflictException(String message) {
        super(message);
    }
}
