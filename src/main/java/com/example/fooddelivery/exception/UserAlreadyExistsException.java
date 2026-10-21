package com.example.fooddelivery.exception;

/**
 * Exception thrown when attempting to register an account with an email that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
