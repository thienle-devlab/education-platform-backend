package com.lethien.auth_service.exception;

/**
 * Exception thrown when email already exists
 */

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
