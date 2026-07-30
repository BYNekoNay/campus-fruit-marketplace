package com.campusfruit.identity.service;

import com.campusfruit.identity.entity.AuditLog;
import com.campusfruit.identity.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * 审计日志服务，记录关键操作到 audit_logs 表。
 * 密码相关内容不会记录到新值/旧值字段中。
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void log(Long actorId, String actorType, String action,
                    String targetType, String targetId,
                    String oldValue, String newValue, String reason) {
        AuditLog entry = new AuditLog();
        entry.setActorId(actorId);
        entry.setActorType(actorType);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setReason(reason);
        entry.setIpAddress(getClientIp());

        auditLogRepository.save(entry);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Failed to get client IP", e);
        }
        return "unknown";
    }
}
