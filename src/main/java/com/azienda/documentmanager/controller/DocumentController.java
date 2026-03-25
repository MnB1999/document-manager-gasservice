package com.azienda.documentmanager.controller;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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

   

    @GetMapping("/expiring")
    public List<Document> getExpiring() {
        return documentService.checkExpiringDocuments();
    }

    
    @GetMapping
    public List<Document> getAll(@RequestParam(defaultValue = "USER") String role) {
        return documentService.getAllAllowedDocuments(role);
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestParam(value = "file", required = false) MultipartFile file, 
                                           @RequestParam("title") String title,
                                           @RequestParam("category") String category,
                                           @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                           @RequestParam("isSpecial") boolean isSpecial) {

        Document savedDoc = documentService.saveDocument(file, title, category, expiryDate, isSpecial);
        return ResponseEntity.ok(savedDoc);

    }

}
