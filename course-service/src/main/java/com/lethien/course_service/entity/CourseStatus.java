package com.lethien.course_service.entity;

/**
 * Trạng thái khóa học — maps to course_status ENUM in PostgreSQL
 */

public enum CourseStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
