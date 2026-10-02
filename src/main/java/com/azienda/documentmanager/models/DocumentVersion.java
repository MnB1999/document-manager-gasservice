package com.azienda.documentmanager.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_versions")
@Getter
@Setter
@NoArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "archived_at", nullable = false, updatable = false)
    private OffsetDateTime archivedAt;

    @Override
    public String toString() {
        return "DocumentVersion{id=" + id + ", archivedAt=" + archivedAt + "}";
    }
}