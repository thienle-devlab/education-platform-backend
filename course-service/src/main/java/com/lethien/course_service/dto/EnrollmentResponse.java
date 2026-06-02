package com.lethien.course_service.dto;

import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.Enrollment;
import com.lethien.course_service.entity.EnrollmentStatus;
import com.lethien.course_service.entity.SkillType;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * EnrollmentResponse — enrollment detail returned to the enrolled user.
 * Used in: GET /enrollments/me, GET /enrollments/{id}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private UUID id;
    private UUID userId;

    // Course summary fields (denormalized for convenience)
    private UUID courseId;
    private String courseTitle;
    private String courseThumbnailUrl;
    private SkillType skillType;

    private EnrollmentStatus status;
    private BigDecimal progressPercent;

    private ZonedDateTime enrolledAt;
    private ZonedDateTime completedAt;
    private ZonedDateTime expiredAt;
    private ZonedDateTime lastAccessedAt;

    // ============================================
    // MAPPER
    // ============================================

    public static EnrollmentResponse from(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUserId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseThumbnailUrl(course.getThumbnailUrl())
                .skillType(course.getSkillType())
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .expiredAt(enrollment.getExpiredAt())
                .lastAccessedAt(enrollment.getLastAccessedAt())
                .build();
    }
}
