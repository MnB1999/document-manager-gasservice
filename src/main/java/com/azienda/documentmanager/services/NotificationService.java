package com.azienda.documentmanager.services;

import com.azienda.documentmanager.models.Document;
import com.azienda.documentmanager.repositories.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final ReminderRecurrenceService recurrenceService;

    @Value("${app.notifications.days-before-expiry}")
    private int daysBeforeExpiry;

    @Value("${app.notifications.renotify-after-days}")
    private int renotifyAfterDays;

    @Value("${app.notifications.standard-recipients}")
    private List<String> standardRecipients;

    @Value("${app.notifications.special-recipients}")
    private List<String> specialRecipients;

    @Async
    @Transactional
    public void notifyExpiringDocuments() {
        List<Document> toNotify = documentRepository
                .findByExpiryDateLessThanEqual(LocalDate.now().plusDays(daysBeforeExpiry))
                .stream()
                .filter(this::shouldNotify)
                .toList();

        if (toNotify.isEmpty()) return;

        List<Document> standard = toNotify.stream().filter(document -> !document.isSpecial()).toList();
        List<Document> special = toNotify.stream().filter(Document::isSpecial).toList();

        try {
            if (!standard.isEmpty()) {
                emailService.sendDeadlineAlert(standardRecipients, standard);
            }
            if (!special.isEmpty()) {
                emailService.sendDeadlineAlert(specialRecipients, special);
            }

        } catch (Exception e) {
            log.error("Errore invio email di scadenza.", e);
            return;
        }

        markAsNotified(toNotify);
        log.info("Notifica inviata con successo per {} documenti", toNotify.size());
    }

    private boolean shouldNotify(Document doc) {
        if (!doc.isNotified()) {
            return true;
        }
        if (doc.getLastNotifiedAt() == null) {
            return false;
        }
        long daysSinceLastNotification = ChronoUnit.DAYS.between(doc.getLastNotifiedAt(), LocalDate.now());
        return daysSinceLastNotification >= renotifyAfterDays;
    }

    private void markAsNotified(List<Document> documents) {
        LocalDate today = LocalDate.now();
        documents.forEach(doc -> {
            doc.setLastNotifiedAt(today);
            if (doc.getFrequencyMonths() != null) {
                // The mail has been sent: the next occurrence starts a new notification cycle
                LocalDate notifiedExpiry = doc.getExpiryDate();
                recurrenceService.advanceExpiryAfter(doc, today);
                doc.setNotified(false);
                log.info("Promemoria ricorrente {} rinnovato: {} -> {}", doc.getId(), notifiedExpiry, doc.getExpiryDate());
            } else {
                doc.setNotified(true);
            }
        });
    }
}
