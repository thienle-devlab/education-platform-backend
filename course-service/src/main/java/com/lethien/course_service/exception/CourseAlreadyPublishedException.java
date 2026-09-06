package com.lethien.course_service.exception;

public class CourseAlreadyPublishedException extends RuntimeException {
    public CourseAlreadyPublishedException(String message) {
        super(message);
    }
}
