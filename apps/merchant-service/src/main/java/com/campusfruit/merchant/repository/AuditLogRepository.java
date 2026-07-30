package com.campusfruit.merchant.repository;

import com.campusfruit.merchant.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);

    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);
}
