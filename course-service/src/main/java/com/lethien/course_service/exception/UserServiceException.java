package com.lethien.course_service.exception;

/**
 * UserServiceException — thrown khi không thể gọi User Service.
 * Maps to HTTP 503 trong GlobalExceptionHandler.
 */
public class UserServiceException extends RuntimeException {
    public UserServiceException(String message) {
        super(message);
    }
}
