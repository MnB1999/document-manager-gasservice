package com.azienda.documentmanager.service;

import com.azienda.documentmanager.exception.ResourceNotFoundException;
import com.azienda.documentmanager.model.*;
import com.azienda.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {


    private final DocumentRepository documentRepository;
    private final AuditService auditService;
    private final StorageService storageService;
    private final SecurityService securityService;


    @Transactional(readOnly = true)
    public Page<Document> getDocumentsForUser(UUID userId, int page, int size) {
        return documentRepository.findByCreatedBy(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Document getDocumentByID(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Il documento con ID " + id + " non esiste."));

        if (doc.isSpecial() && !securityService.isAdmin()) {
            throw new AccessDeniedException("Non hai i permessi per visualizzare questo documento.");
        }
        return doc;
    }

    @Transactional(readOnly = true)
    public Page<Document> getExpiringDocumentsReadOnly(int page, int size) {
        LocalDate limitDate = LocalDate.now().plusDays(21);
        return documentRepository.findByExpiryDateBefore(limitDate, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Document> getAllAllowedDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (securityService.isAdmin()) {
            return documentRepository.findAll(pageable);
        } else {
            return documentRepository.findBySpecialFalse(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<Document> searchAllowedDocuments(String title, String category, LocalDate start, LocalDate end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.searchDocumentsFiltered(title, category, start, end, securityService.isAdmin(), pageable);
    }

    @Transactional
    public Document saveDocument(MultipartFile file, String title, String category, LocalDate expiryDate, boolean isSpecial, String content) {

        if (isSpecial && !securityService.isAdmin()) {
            throw new AccessDeniedException("Operazione negata: solo gli amministratori possono caricare documenti speciali.");
        }

        String fileUrl = (file != null && !file.isEmpty()) ? storageService.uploadFileToSupabase(file) : null;

        Document doc = new Document();
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setExpiryDate(expiryDate);
        doc.setSpecial(isSpecial);
        doc.setFileUrl(fileUrl);
        doc.setContent(content);
        doc.setType(file != null && !file.isEmpty() ? DocumentType.FILE : DocumentType.TEXT_REMINDER);
        doc.setCreatedBy(securityService.getCurrentUserId());

        Document saved = documentRepository.save(doc);
        auditService.logAudit(saved.getId(), saved.getTitle(), AuditAction.UPLOAD);
        return saved;
    }

    @Transactional // Rolls back if upload of new document fails
    public Document renewDocument(UUID documentId, MultipartFile newFile, LocalDate newExpiryDate, String newContent) {
        Optional<Document> doc = documentRepository.findById(documentId);

        if (doc.isEmpty()) {
            throw new ResourceNotFoundException("Documento non trovato con ID: " + documentId);
        }

        Document existingDoc = doc.get();

        if (existingDoc.isSpecial() && !securityService.isAdmin()) {
            throw new AccessDeniedException("Operazione negata: solo gli amministratori possono modificare documenti speciali.");
        }

        if (existingDoc.getFileUrl() != null) {
            DocumentVersion oldVersion = new DocumentVersion();
            oldVersion.setDocument(existingDoc);
            oldVersion.setFileUrl(existingDoc.getFileUrl());
            oldVersion.setExpiryDate(existingDoc.getExpiryDate());
            oldVersion.setArchivedAt(LocalDate.now());
            existingDoc.getHistory().add(oldVersion);
        }

        existingDoc.setExpiryDate(newExpiryDate);
        existingDoc.setNotified(false);
        existingDoc.setLastNotifiedAt(null);
        if (newFile != null && !newFile.isEmpty()) {
            String newFileUrl = storageService.uploadFileToSupabase(newFile);
            existingDoc.setFileUrl(newFileUrl);
            existingDoc.setType(DocumentType.FILE);
        }
        if (newContent != null && !newContent.isBlank()) {
            existingDoc.setContent(newContent);
        }
        Document renewed = documentRepository.save(existingDoc);
        auditService.logAudit(renewed.getId(), renewed.getTitle(), AuditAction.RENEW);
        return renewed;
    }

    @Transactional(readOnly = true)
    public List<DocumentVersion> getDocumentHistory(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato con ID: " + documentId));

        if (doc.isSpecial() && !securityService.isAdmin()) {
            throw new AccessDeniedException("Non hai i permessi per visualizzare questo documento.");
        }
        return doc.getHistory();
    }

    @Transactional
    public void logicalDeleteDocument(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato"));
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDate.now());
        documentRepository.save(doc);
        auditService.logAudit(doc.getId(), doc.getTitle(), AuditAction.DELETE);
    }

    @Transactional
    public void finalizePhysicalDeletion(Document doc) {
        auditService.logAudit(doc.getId(), doc.getTitle(), AuditAction.PHYSICAL_DELETE);
        documentRepository.deleteVersionsByDocumentId(doc.getId());
        documentRepository.physicalDeleteById(doc.getId());
    }
}
