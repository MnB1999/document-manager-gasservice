package com.azienda.documentmanager.repositories;

import com.azienda.documentmanager.models.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Used by the notification job and the /expiring endpoint.
    // The "SpecialFalse" variant is intended for non-admin users; without it,
    // /expiring exposed restricted documents
    List<Document> findByExpiryDateLessThanEqual(LocalDate threshold);
    List<Document> findByExpiryDateLessThanEqualAndSpecialFalse(LocalDate threshold);

    Page<Document> findByCreatedBy(UUID userId, Pageable pageable);
    Page<Document> findByCreatedByAndSpecialFalse(UUID userId, Pageable pageable);
    Page<Document> findBySpecialFalse(Pageable pageable);

    // :title is already sanitized by the service (escaping \ % _),
    // the ESCAPE clause makes the escaping effective.
    @Query("SELECT d FROM Document d WHERE " +
            "(:title IS NULL OR LOWER(d.title) LIKE LOWER(CAST(:title AS string)) ESCAPE '\\') AND " +
            "(:category IS NULL OR d.category = :category) AND " +
            "(:startDate IS NULL OR d.expiryDate >= :startDate) AND " +
            "(:endDate IS NULL OR d.expiryDate <= :endDate) AND " +
            "(:isAdmin = true OR d.special = false)")
    Page<Document> searchDocumentsFiltered(
            @Param("title") String title, @Param("category") String category,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("isAdmin") boolean isAdmin, Pageable pageable);

    // Must also see records with deleted=true,
    @Query(value = "SELECT * FROM documents WHERE deleted = true AND deleted_at <= :threshold", nativeQuery = true)
    List<Document> findReadyForPhysicalDeletion(@Param("threshold") LocalDate threshold);

    // Child versions are deleted by the ON DELETE CASCADE foreign key defined in the schema.
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM documents WHERE id = :id", nativeQuery = true)
    void physicalDeleteById(@Param("id") UUID id);
}