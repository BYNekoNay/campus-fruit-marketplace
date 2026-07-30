package com.campusfruit.review.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByReviewIdOrderByCreatedAtDesc(Long reviewId);
}
