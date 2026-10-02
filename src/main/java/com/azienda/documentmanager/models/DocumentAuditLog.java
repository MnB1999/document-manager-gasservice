package com.azienda.documentmanager.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class DocumentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // No FK
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "document_title")
    private String documentTitle;

    //Null when there is a system action (clean up task)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email")
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Column(name = "performed_at", nullable = false)
    private OffsetDateTime performedAt;
}