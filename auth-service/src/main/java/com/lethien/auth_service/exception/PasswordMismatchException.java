package com.lethien.auth_service.exception;

/**
 * Exception thrown when password and confirm password do not match
 */
public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}
