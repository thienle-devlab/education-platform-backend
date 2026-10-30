package com.lethien.course_service.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * UpdateSectionRequest — partial update, null fields ignored.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSectionRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;
}
