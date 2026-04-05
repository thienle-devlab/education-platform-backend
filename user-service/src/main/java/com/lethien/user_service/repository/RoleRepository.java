package com.lethien.user_service.repository;

import com.lethien.user_service.entity.Role;
import com.lethien.user_service.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // ============================================
    // FIND BY NAME
    // ============================================
    /**
     * Find a role by its RoleType enum.
     * Used in: assigning default STUDENT role on registration,
     *          looking up ADMIN role for permission checks.
     */
    Optional<Role> findByName(RoleType name);

    /**
     * Check if a role exists by name.
     * Used in: validation before assignment.
     */
    boolean existsByName(RoleType name);

    /**
     * Find multiple roles by their names in one query.
     * Used in: bulk role assignment (e.g. assign INSTRUCTOR + MODERATOR at once).
     */
    @Query("""
        SELECT r FROM Role r
        WHERE r.name IN :names
        """)
    List<Role> findAllByNameIn(@Param("names") List<RoleType> names);
}
