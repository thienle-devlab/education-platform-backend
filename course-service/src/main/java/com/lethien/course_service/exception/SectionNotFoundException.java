package com.lethien.course_service.exception;

public class SectionNotFoundException extends RuntimeException {
    public SectionNotFoundException(String message) {
        super(message);
    }
}
