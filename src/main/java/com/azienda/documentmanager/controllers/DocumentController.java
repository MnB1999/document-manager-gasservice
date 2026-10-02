package com.azienda.documentmanager.controllers;

import com.azienda.documentmanager.dtos.DocumentResponse;
import com.azienda.documentmanager.dtos.DocumentVersionResponse;
import com.azienda.documentmanager.models.Document;
import com.azienda.documentmanager.services.DocumentService;
import com.azienda.documentmanager.services.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
    private final StorageService storageService;

    @PostMapping("/upload")
    @Operation(summary = "Carica un documento o crea un promemoria testuale",
            description = "isSpecial marca il documento come riservato: solo gli ADMIN possono " +
                    "impostarlo a true e solo gli ADMIN potranno poi vederlo. Un utente " +
                    "normale che tenta isSpecial=true riceve 403.")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @Parameter(description = "true = riservato, visibile solo agli ADMIN. Solo un ADMIN può impostarlo.")
            @RequestParam("isSpecial") boolean isSpecial,
            @RequestParam(value = "content", required = false) String content,
            @Parameter(description = "Solo per promemoria testuali: la scadenza si rinnova di N mesi a ogni invio della mail.")
            @RequestParam(value = "frequencyMonths", required = false) Integer frequencyMonths) {

        Document savedDoc = documentService.saveDocument(file, title, category, expiryDate, isSpecial, content, frequencyMonths);
        return ResponseEntity.ok(DocumentResponse.from(savedDoc));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Documenti caricati da un utente specifico")
    public ResponseEntity<Page<DocumentResponse>> getUserDocuments(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        Page<DocumentResponse> result = documentService.getDocumentsForUser(userId, safePage, safeSize)
                .map(DocumentResponse::from);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/expiring")
    @Operation(summary = "Documenti in scadenza (filtrati in base al ruolo dell'utente)")
    public ResponseEntity<List<DocumentResponse>> getExpiring() {
        List<DocumentResponse> result = documentService.getExpiringDocuments().stream()
                .map(DocumentResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    @Operation(summary = "Tutti i documenti accessibili all'utente corrente")
    public ResponseEntity<Page<DocumentResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        Page<DocumentResponse> result = documentService.getAllAllowedDocuments(safePage, safeSize)
                .map(DocumentResponse::from);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Storico versioni di un documento")
    public ResponseEntity<List<DocumentVersionResponse>> getHistory(@PathVariable UUID id) {
        List<DocumentVersionResponse> history = documentService.getDocumentHistory(id).stream()
                .map(DocumentVersionResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PostMapping("/versions/{versionId}/download")
    @Operation(summary = "URL firmato per scaricare il file di una versione precedente")
    public ResponseEntity<Map<String, String>> getVersionDownloadUrl(@PathVariable UUID versionId) {
        String fileUrl = documentService.getVersionFileUrlForDownload(versionId);
        String signedUrl = storageService.generateSignedUrl(fileUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("url", signedUrl));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio di un singolo documento")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable UUID id) {
        return ResponseEntity.ok(DocumentResponse.from(documentService.getDocumentById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Ricerca documenti con filtri paginata")
    public ResponseEntity<Page<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        Page<DocumentResponse> results = documentService
                .searchAllowedDocuments(title, category, startDate, endDate, safePage, safeSize)
                .map(DocumentResponse::from);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}/download")
    @Operation(summary = "Genera un URL firmato per scaricare il file")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID id) {
        Document doc = documentService.getDocumentById(id);

        if (doc.getFileUrl() == null) {
            throw new IllegalArgumentException("Questo documento non ha un file associato.");
        }

        String signedUrl = storageService.generateSignedUrl(doc.getFileUrl());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("url", signedUrl));
    }

    @PutMapping("/renew/{id}")
    @Operation(summary = "Rinnova un documento aggiornando scadenza e/o file")
    public ResponseEntity<DocumentResponse> renewDocument(
            @PathVariable UUID id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newExpiryDate,
            @RequestParam(value = "content", required = false) String content) {

        Document renewedDoc = documentService.renewDocument(id, file, newExpiryDate, content);
        return ResponseEntity.ok(DocumentResponse.from(renewedDoc));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminazione logica di un documento")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.logicalDeleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
