package com.azienda.documentmanager.repository;

import com.azienda.documentmanager.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCreatedBy(UUID userId);

    List<Document> findByExpiryDateBefore(LocalDate date);

    List<Document> findBySpecialFalse();

    List<Document> findByCategory(String category);

    List<Document> findByCategoryAndSpecialFalse(String category);

    // Dynamic filter searching for the users, including partial text recognition
    @Query("SELECT d FROM Document d WHERE " +
            "(:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:category IS NULL OR d.category = :category) AND " +
            "(:startDate IS NULL OR d.expiryDate >= :startDate) AND " +
            "(:endDate IS NULL OR d.expiryDate <= :endDate)")
    List<Document> searchDocuments(
            @Param("title") String title,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

}
