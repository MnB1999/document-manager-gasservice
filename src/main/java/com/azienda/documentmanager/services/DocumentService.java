package com.azienda.documentmanager.services;

import com.azienda.documentmanager.exceptions.ResourceNotFoundException;
import com.azienda.documentmanager.models.AuditAction;
import com.azienda.documentmanager.models.Document;
import com.azienda.documentmanager.models.DocumentType;
import com.azienda.documentmanager.models.DocumentVersion;
import com.azienda.documentmanager.repositories.DocumentRepository;
import com.azienda.documentmanager.repositories.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final AuditService auditService;
    private final StorageService storageService;
    private final SecurityService securityService;
    private final DocumentAccessPolicy accessPolicy;
    private final TransactionalStorageCleanup storageCleanup;

    @Value("${app.notifications.days-before-expiry}")
    private int daysBeforeExpiry;

    /* "save" accepts expired documents, unlike "renew":
       we must keep documents for up to 10 years
       because of italian laws */
    @Transactional
    public Document saveDocument(MultipartFile file, String title, String category,
                                 LocalDate expiryDate, boolean isSpecial, String content, Integer frequencyMonths) {

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasContent = content != null && !content.isBlank();

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Il titolo è obbligatorio.");
        }
        if (!hasFile && !hasContent) {
            throw new IllegalArgumentException("Serve un file oppure un contenuto testuale.");
        }

        accessPolicy.assertCanCreateSpecial(isSpecial);

        String fileUrl = hasFile ? storageService.uploadFileToSupabase(file) : null;
        storageCleanup.deleteIfTransactionFails(fileUrl);

            Document doc = new Document();
            doc.setTitle(title);
            doc.setCategory(category);
            doc.setExpiryDate(expiryDate);
            doc.setSpecial(isSpecial);
            doc.setFileUrl(fileUrl);
            doc.setContent(content);
            doc.setFrequencyMonths(frequencyMonths);
            doc.setType(hasFile ? DocumentType.FILE : DocumentType.TEXT_REMINDER);
            doc.setCreatedBy(securityService.getCurrentUserId());

            if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
                doc.setNotified(true);
            }
            Document saved = documentRepository.save(doc);
            auditService.logAudit(saved.getId(), saved.getTitle(), AuditAction.UPLOAD);

            return saved;
    }

    @Transactional
    public Document renewDocument(UUID documentId, MultipartFile newFile,
                                  LocalDate newExpiryDate, String newContent) {


        // Now date is mandatory
        if (newExpiryDate == null) {
            throw new IllegalArgumentException("La nuova data di scadenza è obbligatoria.");
        }
        if (newExpiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La data di scadenza non può essere nel passato.");
        }

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato con ID: " + documentId));

        accessPolicy.assertCanModify(doc);

        boolean fileChanging = newFile != null && !newFile.isEmpty();
        // Blank content does not change existing one (must not remove a text reminder with a renew)
        boolean contentChanging = newContent != null && !newContent.isBlank()
                && !newContent.equals(doc.getContent());
        boolean expiryChanging = !newExpiryDate.equals(doc.getExpiryDate());

        String newFileUrl;

        if (!fileChanging && !contentChanging && !expiryChanging) {
            return doc;
        }

        if (fileChanging || contentChanging) {
            archiveCurrentVersion(doc);
        } else {
            archivePreviousExpiry(doc);
        }

        doc.setExpiryDate(newExpiryDate);
        doc.setNotified(false);
        doc.setLastNotifiedAt(null);

        if (fileChanging) {
            newFileUrl = storageService.uploadFileToSupabase(newFile);
            storageCleanup.deleteIfTransactionFails(newFileUrl);
            doc.setFileUrl(newFileUrl);
            doc.setType(DocumentType.FILE);
        }
        if (contentChanging) {
            doc.setContent(newContent);
        }

        Document renewed = documentRepository.saveAndFlush(doc);

        auditService.logAudit(renewed.getId(), renewed.getTitle(), AuditAction.RENEW);
        return renewed;

    }

    @Transactional(readOnly = true)
    public String getVersionFileUrlForDownload(UUID versionId) {
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Versione non trovata: " + versionId));
        accessPolicy.assertCanView(version.getDocument());
        if (version.getFileUrl() == null) {
            throw new IllegalArgumentException("Questa versione non ha un file associato.");
        }
        return version.getFileUrl();
    }

    // No longer load entire history to reach a single row
    private void archiveCurrentVersion(Document doc) {
        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setFileUrl(doc.getFileUrl());
        version.setContent(doc.getContent());
        version.setExpiryDate(doc.getExpiryDate());
        versionRepository.save(version);
    }

    private void archivePreviousExpiry(Document doc) {
        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setExpiryDate(doc.getExpiryDate());
        versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public Page<Document> getAllAllowedDocuments(int page, int size) {
        Pageable pageable = pageSortedByCreation(page, size);
        return accessPolicy.canViewSpecial()
                ? documentRepository.findAll(pageable)
                : documentRepository.findBySpecialFalse(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Document> getDocumentsForUser(UUID userId, int page, int size) {
        Pageable pageable = pageSortedByCreation(page, size);
        return accessPolicy.canViewSpecial()
                ? documentRepository.findByCreatedBy(userId, pageable)
                : documentRepository.findByCreatedByAndSpecialFalse(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Document getDocumentById(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Il documento con ID " + id + " non esiste."));
        accessPolicy.assertCanView(doc);
        return doc;
    }

    @Transactional(readOnly = true)
    public List<Document> getExpiringDocuments() {
        LocalDate threshold = LocalDate.now().plusDays(daysBeforeExpiry);
        return accessPolicy.canViewSpecial()
                ? documentRepository.findByExpiryDateLessThanEqual(threshold)
                : documentRepository.findByExpiryDateLessThanEqualAndSpecialFalse(threshold);
    }

    @Transactional(readOnly = true)
    public Page<Document> searchAllowedDocuments(String title, String category,
                                                 LocalDate start, LocalDate end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "expiryDate"));
        return documentRepository.searchDocumentsFiltered(
                escapeLike(title), category, start, end, accessPolicy.canViewSpecial(), pageable);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersion> getDocumentHistory(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato con ID: " + documentId));
        accessPolicy.assertCanView(doc);
        return versionRepository.findByDocumentIdOrderByArchivedAtDesc(documentId);
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
        // Children are deleted by FK ON DELETE CASCADE
        documentRepository.physicalDeleteById(doc.getId());
    }

    private Pageable pageSortedByCreation(int page, int size) {
        // Explicit sorting because postgres does not guarantee any order
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // Escapes LIKE wildcard metacharacters in the search input
    // This prevents user-supplied %, _, and \ from being interpreted as SQL LIKE operators
    private String escapeLike(String input) {
        if (input == null) return null;
        String escaped = input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}