package com.azienda.documentmanager.tasks;

import com.azienda.documentmanager.services.NotificationService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeadlineTask {


    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * ?", zone = "Europe/Rome")
    @SchedulerLock(name = "reportExpiringDocuments", lockAtLeastFor = "5m", lockAtMostFor = "14m")
    public void reportExpiringDocuments() {
        notificationService.notifyExpiringDocuments();
    }
}
