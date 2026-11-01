package com.example.fooddelivery.exception;

/**
 * Exception thrown when a resource already exists or conflicts with existing state.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

