package com.azienda.documentmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Records must stay after physical deletion of the document
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    // Denormalised — same reason as above
    @Column(name = "document_title")
    private String documentTitle;

    // Stays null when the action is system-triggered (CleanupTask)
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}