package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    public List<Document> getDocumentsForUser(UUID userId) {
        return documentRepository.findByCreatedBy(userId);
    }
    public List<Document> checkExpiringDocuments() {
        LocalDate limitDate = LocalDate.now().plusDays(21);
        return documentRepository.findByExpiryDateBeforeAndNotifiedFalse(limitDate);
    }

    public List<Document> getAllAllowedDocuments(String userRole) {
        if ("ADMIN".equals(userRole)) {
            
            return documentRepository.findAll();
        } else {
            
            return documentRepository.findBySpecialFalse();
        }
    }
}