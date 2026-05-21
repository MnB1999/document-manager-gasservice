package com.azienda.documentmanager.task;

import com.azienda.documentmanager.exception.StorageException;
import com.azienda.documentmanager.repository.DocumentRepository;
import com.azienda.documentmanager.service.DocumentService;
import com.azienda.documentmanager.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupTask {
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final StorageService storageService;

    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "physicalDeletionTask", lockAtLeastFor = "10m", lockAtMostFor = "50m")
    public void executePhysicalDeletion() {
        LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2);
        var toDelete = documentRepository.findReadyForPhysicalDeletion(twoMonthsAgo);

        for (var doc : toDelete) {
            try {
                List<String> urlsToDelete = new ArrayList<>();
                if (doc.getFileUrl() != null) {
                    urlsToDelete.add(doc.getFileUrl());
                }
                urlsToDelete.addAll(documentRepository.findHistoryFileUrlsByDocumentId(doc.getId()));

                if (!urlsToDelete.isEmpty()) {
                    storageService.deleteFilesFromSupabase(urlsToDelete);
                }

                documentService.finalizePhysicalDeletion(doc);

            } catch (StorageException e) {
                log.error("Eliminazione storage fallita per documento {}: {}", doc.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("Eliminazione fisica fallita per documento {} (DB o altro)", doc.getId(), e);
            }
        }
    }
}