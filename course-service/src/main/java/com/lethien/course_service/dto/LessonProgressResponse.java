package com.lethien.course_service.dto;

import com.lethien.course_service.entity.LessonProgress;
import com.lethien.course_service.entity.ProgressStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * LessonProgressResponse — tiến trình từng bài học trong một enrollment.
 * Used in: GET /enrollments/{id}/progress
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgressResponse {

    private UUID id;
    private UUID enrollmentId;
    private UUID lessonId;
    private String lessonTitle;

    private ProgressStatus status;
    private BigDecimal score;
    private Integer timeSpentSeconds;

    private ZonedDateTime completedAt;
    private ZonedDateTime updatedAt;

    // ============================================
    // MAPPER
    // ============================================

    public static LessonProgressResponse from(LessonProgress lp) {
        return LessonProgressResponse.builder()
                .id(lp.getId())
                .enrollmentId(lp.getEnrollment().getId())
                .lessonId(lp.getLesson().getId())
                .lessonTitle(lp.getLesson().getTitle())
                .status(lp.getStatus())
                .score(lp.getScore())
                .timeSpentSeconds(lp.getTimeSpentSeconds())
                .completedAt(lp.getCompletedAt())
                .updatedAt(lp.getUpdatedAt())
                .build();
    }
}
