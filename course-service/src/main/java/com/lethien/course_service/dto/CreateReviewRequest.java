package com.lethien.course_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * CreateReviewRequest — request tạo review cho khóa học.
 *
 * Điều kiện:
 *   - User phải có enrollment ACTIVE hoặc COMPLETED
 *   - Mỗi user chỉ review 1 lần / khóa học
 *   - rating: 1-5 sao
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Short rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
