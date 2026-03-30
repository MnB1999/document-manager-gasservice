package com.azienda.documentmanager.task;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.service.DocumentService;
import com.azienda.documentmanager.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeadlineTask {

    @Value("${NOTIFICATION_RECIPIENT}")
    private String recipientEmail;

    private final DocumentService documentService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 */1 * ?")
    
    public void reportExpiringDocuments() {
        List<Document> expiring = documentService.checkAndFilterExpiringDocuments();

        if (!expiring.isEmpty()) {
            emailService.sendDeadlineAlert(recipientEmail, expiring);
        }
    }
}
