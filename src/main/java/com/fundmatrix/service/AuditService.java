package com.fundmatrix.service;

import com.fundmatrix.domain.AuditLog;
import com.fundmatrix.domain.User;
import com.fundmatrix.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String action, String entityType, Object recordId, String details) {
        AuditLog log = new AuditLog();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            log.setUserId(user.getId());
            log.setUserName(user.getName());
        } else {
            log.setUserName("system");
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setRecordId(recordId != null ? String.valueOf(recordId) : null);
        log.setDetails(details != null && details.length() > 500 ? details.substring(0, 500) : details);
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }
}
