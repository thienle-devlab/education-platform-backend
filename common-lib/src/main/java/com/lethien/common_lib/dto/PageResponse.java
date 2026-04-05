package com.lethien.common_lib.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * Paginated response wrapper
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Paginated response wrapper")
public class PageResponse<T> {
    /**
     * List of items in current page
     */
    @Schema(
            description = "List of items for current page",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<T> content;

    /**
     * Current page number (0-indexed)
     */
    @Schema(
            description = "Current page number (0-based)",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer pageNumber;

    /**
     * Page size
     */
    @Schema(
            description = "Number of items per page",
            example = "20",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer pageSize;

    /**
     * Total number of elements across all pages
     */
    @Schema(
            description = "Total number of items across all pages",
            example = "100",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long totalElements;

    /**
     * Total number of pages
     */
    @Schema(
            description = "Total number of pages",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalPages;

    /**
     * Is this the first page?
     */
    @Schema(
            description = "Whether this is the first page",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean first;

    /**
     * Is this the last page?
     */
    @Schema(
            description = "Whether this is the last page",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean last;

    /**
     * Does it have next page?
     */
    @Schema(
            description = "Whether there is a next page",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean hasNext;

    /**
     * Does it have previous page?
     */
    @Schema(
            description = "Whether there is a previous page",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean hasPrevious;

    // ============================================
    // STATIC FACTORY METHOD
    // ============================================

    /**
     * Create from Spring Data Page
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Create empty PageResponse
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(0)
                .totalElements(0L)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
