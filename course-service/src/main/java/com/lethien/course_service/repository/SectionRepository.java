package com.lethien.course_service.repository;

import com.lethien.course_service.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findAllByCourseIdOrderByOrderIndexAsc(UUID courseId);

    boolean existsByCourseIdAndOrderIndex(UUID courseId, Integer orderIndex);

    /**
     * Find max order_index in a course — used when appending a new section.
     */
    @Query("SELECT MAX(s.orderIndex) FROM Section s WHERE s.course.id = :courseId")
    Optional<Integer> findMaxOrderIndexByCourseId(@Param("courseId") UUID courseId);

    long countByCourseId(UUID courseId);
}
