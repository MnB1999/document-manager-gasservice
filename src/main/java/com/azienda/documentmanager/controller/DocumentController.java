package com.azienda.documentmanager.controller;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.model.DocumentVersion;
import com.azienda.documentmanager.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Documenti", description = "Gestione documenti, certificazioni e promemoria")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @Operation(summary = "Carica un documento o crea un promemoria testuale")
    public ResponseEntity<Document> upload(@RequestParam(value = "file", required = false) MultipartFile file, // Sometimes we need to add simple textual reminders, not complete files
                                           @RequestParam("title") String title,
                                           @RequestParam("category") String category,
                                           @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                           @RequestParam("isSpecial") boolean isSpecial,
                                           @RequestParam(value = "content", required = false) String content) {

        Document savedDoc = documentService.saveDocument(file, title, category, expiryDate, isSpecial, content);
        return ResponseEntity.ok(savedDoc);
    }


    @GetMapping("/user/{userId}")
    @Operation(summary = "Documenti caricati da un utente specifico")
    public ResponseEntity<Page<Document>> getUserDocuments(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(documentService.getDocumentsForUser(userId, safePage, safeSize));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Documenti in scadenza entro 21 giorni")
    public ResponseEntity<Page<Document>> getExpiring(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(documentService.getExpiringDocumentsReadOnly(safePage, safeSize));
    }

    @GetMapping("/all")
    @Operation(summary = "Tutti i documenti accessibili all'utente corrente")
    public ResponseEntity<Page<Document>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(documentService.getAllAllowedDocuments(safePage, safeSize));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Storico versioni di un documento")
    public ResponseEntity<List<DocumentVersion>> getHistory(@PathVariable UUID id) {
        List<DocumentVersion> history = documentService.getDocumentHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio di un singolo documento")
    public ResponseEntity<Document> getDocumentById(@PathVariable UUID id) {
        Document doc = documentService.getDocumentByID(id);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/search")
    @Operation(summary = "Ricerca documenti con filtri paginata")
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
    @Operation(summary = "Genera un URL firmato per scaricare il file")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID id) {
        Document doc = documentService.getDocumentByID(id);
        String signedUrl = documentService.generateSignedUrl(doc.getFileUrl());
        return ResponseEntity.ok(Map.of("url", signedUrl));
    }

    @PutMapping("/renew/{id}")
    @Operation(summary = "Rinnova un documento aggiornando scadenza e/o file")
    public ResponseEntity<Document> renewDocument(
            @PathVariable UUID id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newExpiryDate,
            @RequestParam(value = "content", required = false) String content) {

        Document renewedDoc = documentService.renewDocument(id, file, newExpiryDate, content);
        return ResponseEntity.ok(renewedDoc);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminazione logica di un documento")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.logicalDeleteDocument(id);
        return ResponseEntity.noContent().build();
    }

}
