package com.lethien.course_service.repository;

import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.CourseLevel;
import com.lethien.course_service.entity.CourseStatus;
import com.lethien.course_service.entity.SkillType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    // ============================================
    // FIND BY SLUG / INSTRUCTOR
    // ============================================

    Optional<Course> findBySlug(String slug);

    Optional<Course> findByIdAndStatus(UUID id, CourseStatus status);

    boolean existsBySlug(String slug);

    Page<Course> findAllByInstructorId (UUID instructorId, Pageable pageable);

    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    // ============================================
    // FILTER QUERIES (published catalog)
    // ============================================

//    @Query("""
//        SELECT c FROM Course c
//        WHERE c.status = 'PUBLISHED'
//          AND (:skillType IS NULL OR c.skillType = :skillType)
//          AND (:level     IS NULL OR c.level     = :level)
//          AND (:maxPrice  IS NULL OR c.price    <= :maxPrice)
//        ORDER BY c.averageRating DESC NULLS LAST, c.totalEnrollments DESC
//        """)
//    Page<Course> findPublished(
//            @Param("skillType")SkillType skillType,
//            @Param("level") CourseLevel level,
//            @Param("maxPrice") BigDecimal maxPrice,
//            Pageable pageable
//            );

    // ============================================
    // FULL-TEXT SEARCH
    // ============================================

    /**
     * Full-text search on title + description using PostgreSQL GIN index.
     * Leverages: idx_courses_fulltext defined in 01-init.sql.
     */
    @Query(value = """
        SELECT * FROM courses c
        WHERE c.status = 'PUBLISHED'
          AND to_tsvector('english', c.title || ' ' || COALESCE(c.description, ''))
              @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(
            to_tsvector('english', c.title || ' ' || COALESCE(c.description, '')),
            plainto_tsquery('english', :searchTerm)
        ) DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM courses c
        WHERE c.status = 'PUBLISHED'
          AND to_tsvector('english', c.title || ' ' || COALESCE(c.description, ''))
              @@ plainto_tsquery('english', :searchTerm)
        """,
            nativeQuery = true)
    Page<Course> fullTextSearch(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Simple LIKE search — fallback for short queries (< 3 chars).
     */
    @Query("""
        SELECT c FROM Course c
        WHERE c.status = 'PUBLISHED'
          AND (LOWER(c.title)       LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR  LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Course> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ============================================
    // STATISTICS
    // ============================================

    long countByStatus(CourseStatus status);

    long countBySkillTypeAndStatus(SkillType skillType, CourseStatus status);
}
