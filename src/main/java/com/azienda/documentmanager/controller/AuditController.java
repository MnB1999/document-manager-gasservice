package com.azienda.documentmanager.controller;

import com.azienda.documentmanager.model.DocumentAuditLog;
import com.azienda.documentmanager.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Registro delle operazioni sui documenti — solo amministratori")
public class AuditController {

    private final DocumentService documentService;

    @GetMapping("/document/{documentId}")
    @Operation(summary = "Cronologia operazioni di un documento")
    public ResponseEntity<List<DocumentAuditLog>> getDocumentAudit(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getAuditLogForDocument(documentId));
    }

    @GetMapping
    @Operation(summary = "Registro completo paginato di tutte le operazioni")
    public ResponseEntity<Page<DocumentAuditLog>> getAllAudit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(documentService.getAllAuditLogs(safePage, safeSize));
    }
}