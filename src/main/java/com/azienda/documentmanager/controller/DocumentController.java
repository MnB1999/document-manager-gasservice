package com.azienda.documentmanager.controller;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.model.DocumentVersion;
import com.azienda.documentmanager.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/user/{userId}")
    public List<Document> getUserDocuments(@PathVariable UUID userId) {
        return documentService.getDocumentsForUser(userId);
    }

    @GetMapping("/expiring")
    public List<Document> getExpiring() {
        return documentService.checkAndFilterExpiringDocuments();
    }

    @GetMapping("/all")
    public List<Document> getAll() {
        return documentService.getAllAllowedDocuments();
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestParam(value = "file", required = false) MultipartFile file, //Non deve essere per forza un file in quanto necessitiamo di dover inserire anche dei promemoria testuali
                                           @RequestParam("title") String title,
                                           @RequestParam("category") String category,
                                           @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                           @RequestParam("isSpecial") boolean isSpecial) {

        Document savedDoc = documentService.saveDocument(file, title, category, expiryDate, isSpecial);
        return ResponseEntity.ok(savedDoc);

    }

    @PutMapping("/renew/{id}")
    public ResponseEntity<Document> renewDocument(
            @PathVariable UUID id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newExpiryDate) {

        Document renewedDoc = documentService.renewDocument(id, file, newExpiryDate);
        return ResponseEntity.ok(renewedDoc);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<DocumentVersion>> getDocumentHistory(@PathVariable UUID id) {
        Optional<Document> docOpt = documentService.getDocumentById(id);
        if (docOpt.isPresent()) {
            return ResponseEntity.ok(docOpt.get().getHistory());
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocumentCompletely(id);
        return ResponseEntity.noContent().build();
    }
}
