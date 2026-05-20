package com.azienda.documentmanager.repository;

import com.azienda.documentmanager.model.DocumentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentAuditLogRepository extends JpaRepository<DocumentAuditLog, UUID> {

    List<DocumentAuditLog> findByDocumentIdOrderByPerformedAtDesc(UUID documentId);

    Page<DocumentAuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);
}