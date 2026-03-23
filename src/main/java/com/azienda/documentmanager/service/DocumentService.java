package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    public List<Document> getDocumentsForUser(UUID userId) {
        return documentRepository.findByCreatedBy(userId);
    }
}