package com.azienda.documentmanager.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class TransactionalStorageCleanup {

    private final StorageService storageService;

    public void deleteIfTransactionFails(String fileName) {
        if (fileName == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            storageService.deleteQuietly(fileName);
                        }
                    }
                });
    }
}
