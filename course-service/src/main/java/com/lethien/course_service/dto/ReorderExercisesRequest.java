package com.lethien.course_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * ReorderExercisesRequest — đổi thứ tự exercises trong lesson.
 * exerciseIds phải chứa đúng tất cả exerciseId của lesson, không trùng, không thiếu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderExercisesRequest {
    @NotNull(message = "exerciseIds is required")
    @Size(min = 1, message = "At least 1 exercise required")
    private List<UUID> exerciseIds;
}
