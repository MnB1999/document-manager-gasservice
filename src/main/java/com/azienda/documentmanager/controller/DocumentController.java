package com.azienda.documentmanager.controller;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/test")
    public String test() {
        return "Il Controller Documenti è attivo e funzionante!";
    }

    @GetMapping("/expiring")
    public List<Document> getExpiring() {
        
        return documentService.checkExpiringDocuments();
    }

    
    @GetMapping
    public List<Document> getAll(@RequestParam(defaultValue = "USER") String role) {
        return documentService.getAllAllowedDocuments(role);
    }
}
