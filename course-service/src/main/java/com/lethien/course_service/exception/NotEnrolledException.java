package com.lethien.course_service.exception;

public class NotEnrolledException extends RuntimeException {
    public NotEnrolledException(String message) {
        super(message);
    }
}
