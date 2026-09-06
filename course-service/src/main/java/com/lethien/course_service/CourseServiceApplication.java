package com.lethien.course_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * CourseServiceApplication — entry point for Course Service.
 *
 * Responsibilities:
 *   - Course catalog management (CRUD, publish, archive)
 *   - Enrollment management + progress tracking
 *   - Exercise submission + auto-grading
 *   - Speaking/Writing submission → AI grading (async via Kafka)
 *
 * Dependencies (other services):
 *   - Auth Service  : JWT validation (shared secret via common-lib)
 *   - User Service  : instructor/user lookup (TODO [STEP-5 Inter-service])
 *   - AI Service    : consumes SpeakingSubmissionCreatedEvent, WritingSubmissionCreatedEvent
 *                     publishes SubmissionGradedEvent back to Course Service
 */

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.lethien.course_service",
        "com.lethien.common_lib"
})
public class CourseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourseServiceApplication.class, args);
	}

}
