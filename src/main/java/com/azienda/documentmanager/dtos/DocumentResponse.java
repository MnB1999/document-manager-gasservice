package com.azienda.documentmanager.dtos;

import com.azienda.documentmanager.models.Document;
import com.azienda.documentmanager.models.DocumentType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String category,
        DocumentType type,
        String content,
        LocalDate expiryDate,
        boolean special,
        Integer frequencyMonths,
        boolean hasFile,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory(),
                doc.getType(),
                doc.getContent(),
                doc.getExpiryDate(),
                doc.isSpecial(),
                doc.getFrequencyMonths(),
                doc.getFileUrl() != null,
                doc.getCreatedBy(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}