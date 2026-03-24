package com.azienda.documentmanager.task;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.service.DocumentService;
import com.azienda.documentmanager.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeadlineTask {

    private final DocumentService documentService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 */3 * ?")
    public void reportExpiringDocuments() {
        List<Document> expiring = documentService.checkExpiringDocuments();

        if (!expiring.isEmpty()) {
            String destinatario = "${NOTIFICATION_RECIPIENT}";
            emailService.sendDeadlineAlert(destinatario, expiring);
        }
    }
}
