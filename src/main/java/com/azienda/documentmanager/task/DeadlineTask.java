package com.azienda.documentmanager.task;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.service.DocumentService;
import com.azienda.documentmanager.service.EmailService;
import com.azienda.documentmanager.service.NotificationService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeadlineTask {

    @Value("${app.notifications.recipient-email}")
    private String recipientEmail;

    private final EmailService emailService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * ?")
    @SchedulerLock(name = "reportExpiringDocuments", lockAtLeastFor = "5m", lockAtMostFor = "14m")
    public void reportExpiringDocuments() {
        List<Document> expiring = notificationService.processAndNotifyExpiringDocuments();

        if (!expiring.isEmpty()) {
            emailService.sendDeadlineAlert(recipientEmail, expiring);
        }
    }
}
