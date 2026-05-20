package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final DocumentRepository documentRepository;

    @Transactional
    public List<Document> processAndNotifyExpiringDocuments() {
        LocalDate today = LocalDate.now();
        List<Document> expiringDocs = documentRepository.findByExpiryDateBefore(today.plusDays(21));
        List<Document> toNotify = new ArrayList<>();

        for (Document doc : expiringDocs) {
            if (!doc.isNotified()) {
                toNotify.add(doc);
                doc.setNotified(true);
                doc.setLastNotifiedAt(today);
            } else if (doc.getLastNotifiedAt() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(doc.getLastNotifiedAt(), today);
                if (days >= 11) {
                    toNotify.add(doc);
                    doc.setLastNotifiedAt(today);
                }
            }
        }

        if (!toNotify.isEmpty()) {
            documentRepository.saveAll(toNotify);
        }
        return toNotify;
    }

}
