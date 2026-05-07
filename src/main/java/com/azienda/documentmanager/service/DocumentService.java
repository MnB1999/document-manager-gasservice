package com.azienda.documentmanager.service;

import com.azienda.documentmanager.exception.ResourceNotFoundException;
import com.azienda.documentmanager.exception.StorageException;
import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.model.DocumentVersion;
import com.azienda.documentmanager.repository.DocumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private final DocumentRepository documentRepository;

    public List<Document> getDocumentsForUser(UUID userId) {
        return documentRepository.findByCreatedBy(userId);
    }

    public List<Document> checkAndFilterExpiringDocuments() {
        LocalDate today = LocalDate.now();

        List<Document> expiringDocs = documentRepository.findByExpiryDateBefore(today.plusDays(21));
        List<Document> toNotify = new ArrayList<>();

        for (Document doc : expiringDocs) {
            if (!doc.isNotified()) {
                toNotify.add(doc);
                doc.setNotified(true);
                doc.setLastNotifiedAt(today);
            } else if (doc.getLastNotifiedAt() != null) {
                long daysSinceLastNotification = java.time.temporal.ChronoUnit.DAYS.between(doc.getLastNotifiedAt(), today);

                if (daysSinceLastNotification >= 11) {
                    toNotify.add(doc);
                    doc.setLastNotifiedAt(today);
                }
            }
        }

        if (!expiringDocs.isEmpty()) {
            documentRepository.saveAll(expiringDocs);
        }

        return toNotify;
    }

    public List<Document> getAllAllowedDocuments() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return documentRepository.findAll();
        } else {
            return documentRepository.findBySpecialFalse();
        }
    }

    public Document saveDocument(MultipartFile file, String title, String category, LocalDate expiryDate, boolean isSpecial) {

        if (isSpecial && !isAdmin()) {
            throw new AccessDeniedException("Operazione negata: solo gli amministratori possono caricare documenti speciali.");
        }

        String fileUrl = (file != null && !file.isEmpty()) ? uploadFileToSupabase(file) : null;

        Document doc = new Document();
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setExpiryDate(expiryDate);
        doc.setSpecial(isSpecial);
        doc.setFileUrl(fileUrl);
        doc.setType("FILE");
        doc.setCreatedBy(getCurrentUserId());

        return documentRepository.save(doc);
    }

    private String uploadFileToSupabase(MultipartFile file) {
        
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String uploadUrl = supabaseUrl + "/storage/v1/object/aziendale_docs/" + fileName;

        RestClient restClient = RestClient.create();

        try {
            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(file.getResource())
                    .retrieve()
                    .toBodilessEntity();

            
            return supabaseUrl + "/storage/v1/object/public/aziendale_docs/" + fileName;
        } catch (Exception e) {
            throw new StorageException("Impossibile caricare il file su Supabase: " + e.getMessage());
        }
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // On supabase "sub" is the unique user ID
            String supabaseUuid = jwtAuth.getTokenAttributes().get("sub").toString();
            return UUID.fromString(supabaseUuid);
        }

        throw new RuntimeException("Utente non autenticato o Token non valido");
    }

    @Transactional // Rolls back if upload of new document fails
    public Document renewDocument(UUID documentId, MultipartFile newFile, LocalDate newExpiryDate) {
        Optional<Document> doc = documentRepository.findById(documentId);

        if (doc.isEmpty()) {
            throw new ResourceNotFoundException("Documento non trovato con ID: " + documentId);
        }

        Document existingDoc = doc.get();

        if (existingDoc.isSpecial() && !isAdmin()) {
            throw new AccessDeniedException("Operazione negata: solo gli amministratori possono modificare documenti speciali.");
        }

        if (existingDoc.getFileUrl() != null) {
            DocumentVersion oldVersion = new DocumentVersion();
            oldVersion.setDocument(existingDoc);
            oldVersion.setFileUrl(existingDoc.getFileUrl());
            oldVersion.setExpiryDate(existingDoc.getExpiryDate());
            oldVersion.setArchivedAt(LocalDate.now());
            existingDoc.getHistory().add(oldVersion);
        }

        String newFileUrl = null;
        if (newFile != null && !newFile.isEmpty()) {
            newFileUrl = uploadFileToSupabase(newFile);
            existingDoc.setFileUrl(newFileUrl);
        }

        existingDoc.setExpiryDate(newExpiryDate);
        existingDoc.setNotified(false);
        existingDoc.setLastNotifiedAt(null);
        existingDoc.setType("FILE");

        return documentRepository.save(existingDoc);
    }

    public Optional<Document> getDocumentById(UUID id) { return documentRepository.findById(id); }

    private void deleteFileFromSupabase(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            String bucketName = "aziendale_docs";
            String[] parts = fileUrl.split("/");
            String fileName = parts[parts.length - 1];

            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName;

            RestClient restClient = RestClient.create();

           // Json body with list of files to delete
            Map<String, Object> body = Map.of("prefixes", List.of(fileName));

            restClient.method(HttpMethod.DELETE)
                    .uri(deleteUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            System.err.println("Impossibile eliminare il file dal database: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteDocumentCompletely(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento non trovato"));

        // Gets old and current URLs for the files we want to delete
        List<String> urlsToDelete = new ArrayList<>();
        if (doc.getFileUrl() != null) urlsToDelete.add(doc.getFileUrl());

        doc.getHistory().forEach(version -> {
            if (version.getFileUrl() != null) urlsToDelete.add(version.getFileUrl());
        });

        urlsToDelete.forEach(this::deleteFileFromSupabase);

        documentRepository.delete(doc);
    }

    public List<DocumentVersion> getDocumentHistory(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato con ID: " + documentId));

        if (doc.isSpecial()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                throw new AccessDeniedException("Non hai i permessi per visualizzare questo documento.");
            }
        }

        List<DocumentVersion> history = doc.getHistory();
        history.sort((v1, v2) -> {
            if (v1.getArchivedAt() == null || v2.getArchivedAt() == null) return 0;
            return v2.getArchivedAt().compareTo(v1.getArchivedAt());
        });
        return history;

        /* Could also write it like this, which I think is better but, at least to me, less readable
         history.sort(Comparator.comparing(DocumentVersion::getArchivedAt, Comparator.nullsLast(Comparator.reverseOrder())));
         */
    }

    public Document getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Il documento con ID " + id + " non esiste."));
    }
}
