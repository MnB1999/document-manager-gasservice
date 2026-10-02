package com.azienda.documentmanager.dtos;

import com.azienda.documentmanager.models.DocumentVersion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id,
        String content,
        LocalDate expiryDate,
        boolean hasFile,
        OffsetDateTime archivedAt
) {
    public static DocumentVersionResponse from(DocumentVersion version) {
        return new DocumentVersionResponse(
                version.getId(),
                version.getContent(),
                version.getExpiryDate(),
                version.getFileUrl() != null,
                version.getArchivedAt()
        );
    }
}