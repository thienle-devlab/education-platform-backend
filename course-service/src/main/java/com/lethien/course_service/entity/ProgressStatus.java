package com.lethien.course_service.entity;

/**
 * Trạng thái tiến trình bài học — maps to progress_status ENUM in PostgreSQL
 */

public enum ProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
