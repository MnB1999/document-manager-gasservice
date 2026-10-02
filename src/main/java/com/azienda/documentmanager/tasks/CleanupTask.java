package com.azienda.documentmanager.tasks;

import com.azienda.documentmanager.exceptions.StorageException;
import com.azienda.documentmanager.repositories.DocumentRepository;
import com.azienda.documentmanager.repositories.DocumentVersionRepository;
import com.azienda.documentmanager.services.DocumentService;
import com.azienda.documentmanager.services.StorageService;
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
    private final DocumentVersionRepository versionRepository;
    private final DocumentService documentService;
    private final StorageService storageService;

    @Scheduled(cron = "0 0 2 * * ?", zone = "Europe/Rome")
    @SchedulerLock(name = "physicalDeletionTask", lockAtLeastFor = "10m", lockAtMostFor = "50m")
    public void executePhysicalDeletion() {
        LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2);
        var toDelete = documentRepository.findReadyForPhysicalDeletion(twoMonthsAgo);

        for (var doc : toDelete) {
            try {
                List<String> filesToDelete = new ArrayList<>();
                if (doc.getFileUrl() != null) {
                    filesToDelete.add(doc.getFileUrl());
                }
                filesToDelete.addAll(versionRepository.findFileUrlsByDocumentId(doc.getId()));

                if (!filesToDelete.isEmpty()) {
                    storageService.deleteFilesFromSupabase(filesToDelete);
                }

                documentService.finalizePhysicalDeletion(doc);

            } catch (StorageException e) {
                log.error("Eliminazione storage fallita per documento {}: rimandata al prossimo ciclo.", doc.getId(), e);
            } catch (Exception e) {
                log.error("Eliminazione fisica fallita per documento {} (DB o altro)", doc.getId(), e);
            }
        }
    }
}