package com.azienda.documentmanager.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;
    private String category;
    private String type;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_notified")
    private boolean notified = false;

    @Column(name = "is_special")
    private boolean special = false;
}