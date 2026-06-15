package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final DocumentRepository documentRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<Document> getDocumentsToNotify() {
        return documentRepository.findByExpiryDateLessThanEqual(LocalDate.now().plusDays(21))
                  .stream()
                  .filter(this::shouldNotify)
                  .toList();
    }

    private boolean shouldNotify(Document doc) {
        if (!doc.isNotified()) {
            return true;
        }
        if (doc.getLastNotifiedAt() == null) {
            return false;
        }
        long daysSinceLastNotification = ChronoUnit.DAYS.between(doc.getLastNotifiedAt(), LocalDate.now());
        return daysSinceLastNotification >= 11;
    }

    @Async
    @Transactional
    public void sendNotification(String recipientEmail, List<Document> toNotify) {
        if (toNotify.isEmpty()) return;

        try {
            // 1. Attempt network call first
            emailService.sendDeadlineAlert(recipientEmail, toNotify);
            // 2. Only if the email succeeds, mutate the DB
            markAsNotified (toNotify);
            log.info("Notifica inviata con successo per {} documenti", toNotify.size());
        } catch (Exception e) {
            log.error("Errore invio email di scadenza. Database non aggiornato, nuovo tentativo previsto al prossimo ciclo.", e);
        }
    }

    private void markAsNotified(List<Document> documents) {
        LocalDate today = LocalDate.now();
        documents.forEach(doc -> {
            doc.setNotified(true);
            doc.setLastNotifiedAt(today);
        });
        documentRepository.saveAll(documents);
    }
}
