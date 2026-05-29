package com.azienda.documentmanager.repository;

import com.azienda.documentmanager.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Spring data builds the derived query based on its name, what do you know...
    // Non-pageable like before used to notify so I need the whole list of documents
    List<Document> findByExpiryDateLessThanEqual(LocalDate treshold);

    // Pageable variants used by the UI endpoints
    Page<Document> findByCreatedBy(UUID userId, Pageable pageable);
    Page<Document> findByCreatedByAndSpecialFalse(UUID userId, Pageable pageable);
    Page<Document> findBySpecialFalse(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE " +
            "(:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:category IS NULL OR d.category = :category) AND " +
            "(:startDate IS NULL OR d.expiryDate >= :startDate) AND " +
            "(:endDate IS NULL OR d.expiryDate <= :endDate) AND " +
            "(:isAdmin = true OR d.special = false)")
    Page<Document> searchDocumentsFiltered(
            @Param("title") String title, @Param("category") String category,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("isAdmin") boolean isAdmin, Pageable pageable);

    @Query(value = "SELECT * FROM documents WHERE deleted = true AND deleted_at <= :threshold", nativeQuery = true)
    List<Document> findReadyForPhysicalDeletion(@Param("threshold") LocalDate threshold);

    @Query(value = "SELECT file_url FROM document_versions WHERE document_id = :documentId AND file_url IS NOT NULL", nativeQuery = true)
    List<String> findHistoryFileUrlsByDocumentId(@Param("documentId") UUID documentId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM document_versions WHERE document_id = :id", nativeQuery = true)
    void deleteVersionsByDocumentId(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM documents WHERE id = :id", nativeQuery = true)
    void physicalDeleteById(@Param("id") UUID id);
}