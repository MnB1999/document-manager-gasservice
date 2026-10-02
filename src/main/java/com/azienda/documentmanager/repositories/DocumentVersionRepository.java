package com.azienda.documentmanager.repositories;

import com.azienda.documentmanager.models.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findByDocumentIdOrderByArchivedAtDesc(UUID documentId);

    // Used by cleanup task to group files that need to be eliminated from storage
    // before physical deletion (with FK cascade)
    @Query("SELECT v.fileUrl FROM DocumentVersion v WHERE v.document.id = :documentId AND v.fileUrl IS NOT NULL")
    List<String> findFileUrlsByDocumentId(@Param("documentId") UUID documentId);
}
