package com.azienda.documentmanager.service;

import com.azienda.documentmanager.exception.ResourceNotFoundException;
import com.azienda.documentmanager.exception.StorageException;
import com.azienda.documentmanager.model.*;
import com.azienda.documentmanager.repository.DocumentAuditLogRepository;
import com.azienda.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {



    private final StorageService storageService;

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private final DocumentRepository documentRepository;
    private final DocumentAuditLogRepository auditLogRepository;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // On supabase "sub" is the unique user ID
            String supabaseUuid = jwtAuth.getTokenAttributes().get("sub").toString();
            return UUID.fromString(supabaseUuid);
        }

        throw new AuthenticationCredentialsNotFoundException("Utente non autenticato o token non valido");
    }

    private UUID getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null; // Null in cases like cleanup task (system triggered)
        }
    }

    private void logAudit(UUID documentId, String documentTitle, AuditAction action) {
        DocumentAuditLog entry = new DocumentAuditLog();
        entry.setDocumentId(documentId);
        entry.setDocumentTitle(documentTitle);
        entry.setUserId(getCurrentUserIdOrNull());
        entry.setAction(action);
        entry.setPerformedAt(LocalDateTime.now());
        auditLogRepository.save(entry);
    }


    @Transactional(readOnly = true)
    public Page<Document> getDocumentsForUser(UUID userId, int page, int size) {
        return documentRepository.findByCreatedBy(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Document getDocumentByID(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Il documento con ID " + id + " non esiste."));

        if (doc.isSpecial() && !isAdmin()) {
            throw new AccessDeniedException("Non hai i permessi per visualizzare questo documento.");
        }
        return doc;
    }

    @Transactional(readOnly = true)
    public Page<Document> getExpiringDocumentsReadOnly(int page, int size) {
        LocalDate limitDate = LocalDate.now().plusDays(21);
        return documentRepository.findByExpiryDateBefore(limitDate, PageRequest.of(page, size));
    }

    @Transactional
    public List<Document> processAndNotifyExpiringDocuments() {
        LocalDate today = LocalDate.now();
        List<Document> expiringDocs = documentRepository.findByExpiryDateBefore(today.plusDays(21));
        List<Document> toNotify = new ArrayList<>();

        for (Document doc : expiringDocs) {
            if (!doc.isNotified()) {
                toNotify.add(doc);
                doc.setNotified(true);
                doc.setLastNotifiedAt(today);
            } else if (doc.getLastNotifiedAt() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(doc.getLastNotifiedAt(), today);
                if (days >= 11) {
                    toNotify.add(doc);
                    doc.setLastNotifiedAt(today);
                }
            }
        }

        if (!toNotify.isEmpty()) {
            documentRepository.saveAll(toNotify);
        }
        return toNotify;
    }

    @Transactional(readOnly = true)
    public Page<Document> getAllAllowedDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (isAdmin()) {
            return documentRepository.findAll(pageable);
        } else {
            return documentRepository.findBySpecialFalse(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<Document> searchAllowedDocuments(String title, String category, LocalDate start, LocalDate end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.searchDocumentsFiltered(title, category, start, end, isAdmin(), pageable);
    }

    @Transactional
    public Document saveDocument(MultipartFile file, String title, String category, LocalDate expiryDate, boolean isSpecial, String content) {

        if (isSpecial && !isAdmin()) {
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
        doc.setCreatedBy(getCurrentUserId());

        Document saved = documentRepository.save(doc);
        logAudit(saved.getId(), saved.getTitle(), AuditAction.UPLOAD);
        return saved;
    }

    @Transactional // Rolls back if upload of new document fails
    public Document renewDocument(UUID documentId, MultipartFile newFile, LocalDate newExpiryDate, String newContent) {
        Optional<Document> doc = documentRepository.findById(documentId);

        if (doc.isEmpty()) {
            throw new ResourceNotFoundException("Documento non trovato con ID: " + documentId);
        }

        Document existingDoc = doc.get();

        if (existingDoc.isSpecial() && !isAdmin()) {
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
        logAudit(renewed.getId(), renewed.getTitle(), AuditAction.RENEW);
        return renewed;
    }

    @Transactional(readOnly = true)
    public List<DocumentVersion> getDocumentHistory(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato con ID: " + documentId));

        if (doc.isSpecial() && !isAdmin()) {
            throw new AccessDeniedException("Non hai i permessi per visualizzare questo documento.");
        }
        return doc.getHistory();

        /*I'm leaving this older comment below just for future reference on how the project was built
        But I don't need this anymore since I moved the ordering directly to the query
         */
        /* (old comment)Could also write it like this, which I think is better but, at least to me, less readable
         history.sort(Comparator.comparing(DocumentVersion::getArchivedAt, Comparator.nullsLast(Comparator.reverseOrder())));
         */
    }

    @Transactional
    public void logicalDeleteDocument(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato"));
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDate.now());
        documentRepository.save(doc);
        logAudit(doc.getId(), doc.getTitle(), AuditAction.DELETE);
    }

    @Transactional
    public void executePhysicalDeletionForDocument(Document doc) {

        // Gets old and current URLs for the files we want to delete
        List<String> urlsToDelete = new ArrayList<>();
        if (doc.getFileUrl() != null) urlsToDelete.add(doc.getFileUrl());

        doc.getHistory().forEach(version -> {
            if (version.getFileUrl() != null) urlsToDelete.add(version.getFileUrl());
        });

        urlsToDelete.forEach(url -> storageService.deleteFileFromSupabase(url));

        logAudit(doc.getId(), doc.getTitle(), AuditAction.PHYSICAL_DELETE);
        documentRepository.delete(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentAuditLog> getAuditLogForDocument(UUID documentId) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Permesso negato");
        }
        return auditLogRepository.findByDocumentIdOrderByPerformedAtDesc(documentId);
    }

    @Transactional(readOnly = true)
    public Page<DocumentAuditLog> getAllAuditLogs(int page, int size) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Permesso negato");
        }
        return auditLogRepository.findAllByOrderByPerformedAtDesc(PageRequest.of(page, size));
    }
}
