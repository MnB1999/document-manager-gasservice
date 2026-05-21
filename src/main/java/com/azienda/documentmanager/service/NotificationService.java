package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final DocumentRepository documentRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<Document> getDocumentsToNotify() {
        List<Document> expiringDocs = documentRepository.findByExpiryDateBetween(LocalDate.now(), LocalDate.now().plusDays(21));
        List<Document> toNotify = new ArrayList<>();

        for (Document doc : expiringDocs) {
            if (!doc.isNotified()) {
                toNotify.add(doc);
            } else if (doc.getLastNotifiedAt() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(doc.getLastNotifiedAt(), LocalDate.now());
                if (days >= 11) {
                    toNotify.add(doc);
                }
            }
        }
        return toNotify;
    }

    @Async
    @Transactional
    public void notifyAndUpdateState(String recipientEmail, List<Document> toNotify) {
        if (toNotify.isEmpty()) return;

        try {
            // 1. Attempt network call first
            emailService.sendDeadlineAlert(recipientEmail, toNotify);

            // 2. Only if the email succeeds, mutate the DB
            toNotify.forEach(doc -> {
                doc.setNotified(true);
                doc.setLastNotifiedAt(LocalDate.now());
            });
            documentRepository.saveAll(toNotify);

            log.info("Notifica inviata con successo per {} documenti", toNotify.size());
        } catch (Exception e) {
            log.error("Errore invio email di scadenza. Database non aggiornato, nuovo tentativo previsto al prossimo ciclo.", e);
        }
    }
}
