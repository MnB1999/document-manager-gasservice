package com.azienda.documentmanager.services;


import com.azienda.documentmanager.models.AuditAction;
import com.azienda.documentmanager.models.DocumentAuditLog;
import com.azienda.documentmanager.repositories.DocumentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final DocumentAuditLogRepository auditLogRepository;
    private final SecurityService securityService;

    public void logAudit(UUID documentId, String documentTitle, AuditAction action) {
        DocumentAuditLog entry = new DocumentAuditLog();
        entry.setDocumentId(documentId);
        entry.setDocumentTitle(documentTitle);
        entry.setUserId(securityService.getCurrentUserIdOrNull());
        entry.setUserEmail(securityService.getCurrentUserEmailOrNull());
        entry.setAction(action);
        entry.setPerformedAt(OffsetDateTime.now());
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<DocumentAuditLog> getAuditLogForDocument(UUID documentId) {
        if (!securityService.isAdmin()) {
            throw new AccessDeniedException("Permesso negato");
        }
        return auditLogRepository.findByDocumentIdOrderByPerformedAtDesc(documentId);
    }

    @Transactional(readOnly = true)
    public Page<DocumentAuditLog> getAllAuditLogs(int page, int size) {
        if (!securityService.isAdmin()) {
            throw new AccessDeniedException("Permesso negato");
        }
        return auditLogRepository.findAllByOrderByPerformedAtDesc(PageRequest.of(page, size));
    }

}
