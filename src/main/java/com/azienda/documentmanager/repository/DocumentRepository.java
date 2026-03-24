package com.azienda.documentmanager.repository;

import com.azienda.documentmanager.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCreatedBy(UUID userId);

    
    
    List<Document> findByExpiryDateBeforeAndNotifiedFalse(LocalDate date);

    List<Document> findBySpecialFalse();
}