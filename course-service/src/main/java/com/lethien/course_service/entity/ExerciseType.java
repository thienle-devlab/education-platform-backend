package com.lethien.course_service.entity;

/**
 * 10 loại bài tập cho 4 kỹ năng — maps to exercise_type ENUM in PostgreSQL
 */
public enum ExerciseType {
    MULTIPLE_CHOICE,    // Trắc nghiệm (Listening / Reading)
    FILL_IN_THE_BLANK,  // Điền vào chỗ trống
    TRUE_FALSE,         // Đúng / Sai
    MATCHING,           // Nối cột
    ORDERING,           // Sắp xếp thứ tự
    SHORT_ANSWER,       // Trả lời ngắn (Reading / Writing)
    ESSAY,              // Viết bài luận → tạo writing_submissions
    SPEAKING_RECORD,    // Ghi âm phát âm  → tạo speaking_submissions
    SPEAKING_REPEAT,    // Nhắc lại câu    → tạo speaking_submissions
    DICTATION           // Nghe và viết lại (Listening)
}
