package com.lethien.user_service.entity;

import lombok.Getter;

/**
 * Role type enum
 * Maps to: role_type enum in database
 */
@Getter
public enum RoleType {
    STUDENT("Student", "Regular student user with access to courses and learning materials"),
    INSTRUCTOR("Instructor", "Instructor who can create and manage courses"),
    ADMIN("Admin", "System administrator with full access to all features"),
    MODERATOR("Moderator", "Content moderator who can review and manage user content");

    private final String displayName;
    private final String description;

    RoleType(String displayName, String description){
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Check if role has admin privileges
     */
    public boolean hasAdminPrivileges(){
        return this == ADMIN;
    }

    /**
     * Check if role can manage content
     */
    public boolean canManageContent() {
        return this == ADMIN || this == MODERATOR || this == INSTRUCTOR;
    }

    /**
     * Check if role can create courses
     */
    public boolean canCreateCourses() {
        return this == ADMIN || this == INSTRUCTOR;
    }
}
