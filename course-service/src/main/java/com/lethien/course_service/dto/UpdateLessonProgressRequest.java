package com.lethien.course_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * UpdateLessonProgressRequest — request to update lesson progress.
 * Called via POST /enrollments/{enrollmentId}/lessons/{lessonId}/progress
 *
 * Flow:
 *   1. First call  → status: NOT_STARTED → IN_PROGRESS
 *   2. Final call  → status: IN_PROGRESS → COMPLETED
 *      trigger_progress_update sẽ tự tính lại enrollments.progress_percent
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLessonProgressRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    /** Số giây tích lũy thêm trong lần này (không phải tổng) */
    @Min(value = 0, message = "Time spent must be >= 0")
    private Integer timeSpentSeconds = 0;

    /**
     * TRUE = đánh dấu bài học hoàn thành.
     * FALSE / null = chỉ cập nhật timeSpent, chuyển sang IN_PROGRESS nếu chưa bắt đầu.
     */
    private Boolean markCompleted = false;
}
