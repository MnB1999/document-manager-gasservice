package com.azienda.documentmanager.controller;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.model.DocumentVersion;
import com.azienda.documentmanager.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestParam(value = "file", required = false) MultipartFile file, // Sometimes we need to add simple textual reminders, not complete files
                                           @RequestParam("title") String title,
                                           @RequestParam("category") String category,
                                           @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                           @RequestParam("isSpecial") boolean isSpecial) {

        Document savedDoc = documentService.saveDocument(file, title, category, expiryDate, isSpecial);
        return ResponseEntity.ok(savedDoc);

    }


    @GetMapping("/user/{userId}")
    public List<Document> getUserDocuments(@PathVariable UUID userId) {
        return documentService.getDocumentsForUser(userId);
    }

    @GetMapping("/expiring")
    public List<Document> getExpiring() {
        return documentService.getExpiringDocumentsReadOnly();
    }

    @GetMapping("/all")
    public List<Document> getAll() {
        return documentService.getAllAllowedDocuments();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<DocumentVersion>> getHistory(@PathVariable UUID id) {
        List<DocumentVersion> history = documentService.getDocumentHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable UUID id) {
        Document doc = documentService.getDocumentByID(id);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Document>> searchDocuments(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<Document> results = documentService.searchAllowedDocuments(title, category, startDate, endDate, safePage, safeSize);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID id) {
        Document doc = documentService.getDocumentByID(id);
        String signedUrl = documentService.generateSignedUrl(doc.getFileUrl());
        return ResponseEntity.ok(Map.of("url", signedUrl));
    }

    @PutMapping("/renew/{id}")
    public ResponseEntity<Document> renewDocument(
            @PathVariable UUID id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newExpiryDate) {

        Document renewedDoc = documentService.renewDocument(id, file, newExpiryDate);
        return ResponseEntity.ok(renewedDoc);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.logicalDeleteDocument(id);
        return ResponseEntity.noContent().build();
    }

}
