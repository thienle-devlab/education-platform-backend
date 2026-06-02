package com.lethien.course_service.entity;

/**
 * Trạng thái AI chấm bài — maps to submission_status ENUM in PostgreSQL
 * Dùng cho speaking_submissions và writing_submissions
 */

public enum SubmissionStatus {
    PENDING,     // Chờ AI chấm
    PROCESSING,  // Đang xử lý
    GRADED,      // Đã chấm xong
    FAILED       // Lỗi khi chấm
}
