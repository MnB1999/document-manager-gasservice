package com.azienda.documentmanager.task;

import com.azienda.documentmanager.repository.DocumentRepository;
import com.azienda.documentmanager.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupTask {
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "physicalDeletionTask", lockAtLeastFor = "10m", lockAtMostFor = "50m")
    public void executePhysicalDeletion() {
        LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2);
        var toDelete = documentRepository.findReadyForPhysicalDeletion(twoMonthsAgo);

        for (var doc : toDelete) {
            documentService.executePhysicalDeletionForDocument(doc.getId());
        }
    }
}