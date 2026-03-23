package com.azienda.documentmanager.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "documents")
@Data 
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "created_by")
    private UUID createdBy;
}
