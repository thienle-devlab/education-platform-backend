package com.lethien.course_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * ReorderSectionsRequest — đổi thứ tự sections trong course.
 *
 * sectionIds phải chứa đúng tất cả sectionId của course đó,
 * không được thiếu hoặc thừa.
 *
 * Ví dụ: [sectionId3, sectionId1, sectionId2]
 * → Section 3 sẽ có orderIndex=0, Section 1 orderIndex=1, Section 2 orderIndex=2
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSectionsRequest {

    @NotNull(message = "sectionIds is required")
    @Size(min = 1, message = "At least 1 section required")
    private List<UUID> sectionIds;
}
