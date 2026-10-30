package com.lethien.course_service.dto;

import com.lethien.course_service.entity.CourseReview;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * ReviewResponse — review detail trả về cho client.
 * Used in: GET /courses/{id}/reviews, POST /courses/{id}/reviews
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private UUID id;
    private UUID userId;
    private UUID courseId;

    private Short rating;
    private String comment;
    private boolean isVerifiedPurchase;

    private ZonedDateTime createdAt;

    // ============================================
    // MAPPER
    // ============================================

    public static ReviewResponse from(CourseReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .courseId(review.getCourse().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
