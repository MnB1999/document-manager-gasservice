package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.AuditAction;
import com.azienda.documentmanager.model.DocumentAuditLog;
import com.azienda.documentmanager.repository.DocumentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        entry.setAction(action);
        entry.setPerformedAt(LocalDateTime.now());
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
