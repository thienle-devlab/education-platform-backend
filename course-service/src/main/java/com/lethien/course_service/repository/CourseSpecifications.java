package com.lethien.course_service.repository;

import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.CourseLevel;
import com.lethien.course_service.entity.CourseStatus;
import com.lethien.course_service.entity.SkillType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Provides reusable specifications for filtering courses.
 * These specifications can be combined dynamically to build course queries.
 */
public class CourseSpecifications {

    /**
     * Filters courses that are currently published.
     *
     * @return a specification that matches courses with PUBLISHED status
     */
    public static Specification<Course> published() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        CourseStatus.PUBLISHED
                );
    }

    /**
     * Filters courses by skill type when a skill type is provided.
     *
     * @param skillType the skill type to filter by, or null to skip this filter
     * @return a specification that matches the given skill type,
     *         or no additional restriction when skillType is null
     */
    public static Specification<Course> hasSkillType(SkillType skillType) {
        return (root, query, criteriaBuilder) ->
                skillType == null
                        ? null
                        : criteriaBuilder.equal(root.get("skillType"), skillType);
    }

    /**
     * Filters courses by difficulty level when a level is provided.
     *
     * @param level the course difficulty level to filter by,
     *              or null to skip this filter
     * @return a specification that matches the given level,
     *         or no additional restriction when level is null
     */
    public static Specification<Course> hasLevel(CourseLevel level) {
        return (root, query, criteriaBuilder) ->
                level == null
                        ? null
                        : criteriaBuilder.equal(root.get("level"), level);
    }

    /**
     * Filters courses whose price is less than or equal to the specified maximum price.
     *
     * @param maxPrice the maximum course price, or null to skip this filter
     * @return a specification that matches courses within the maximum price,
     *         or no additional restriction when maxPrice is null
     */
    public static Specification<Course> priceAtMost(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
                maxPrice == null
                        ? null
                        : criteriaBuilder.lessThanOrEqualTo(
                                root.get("price"), maxPrice
                );
    }
}
