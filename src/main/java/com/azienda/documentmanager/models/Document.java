package com.azienda.documentmanager.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private DocumentType type;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_notified")
    private boolean notified = false;

    @Column(name = "last_notified_at")
    private LocalDate lastNotifiedAt;

    @Column(name = "is_special")
    private boolean special = false;

    @Column(name = "deleted")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    // Only for TEXT_REMINDER: null = not recurring
    @Column(name = "frequency_months")
    private Integer frequencyMonths;

    @Override
    public String toString() {
        return "Document{id=" + id + ", title='" + title + "', type=" + type + "}";
    }
}