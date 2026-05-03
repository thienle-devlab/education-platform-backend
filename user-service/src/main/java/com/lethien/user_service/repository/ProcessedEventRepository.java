package com.lethien.user_service.repository;

import com.lethien.user_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
