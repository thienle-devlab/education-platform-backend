package com.lethien.course_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * ReorderLessonsRequest — đổi thứ tự lessons trong section.
 * lessonIds phải chứa đúng tất cả lessonId của section.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderLessonsRequest {

    @NotNull(message = "lessonIds is required")
    @Size(min = 1, message = "At least 1 lesson required")
    private List<UUID> lessonIds;
}
