package com.example.fooddelivery.exception;

/**
 * Exception thrown when a requested domain entity is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
