package com.azienda.documentmanager.service;

import com.azienda.documentmanager.exception.ResourceNotFoundException;
import com.azienda.documentmanager.exception.StorageException;
import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.model.DocumentType;
import com.azienda.documentmanager.model.DocumentVersion;
import com.azienda.documentmanager.repository.DocumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {


    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${app.storage.bucket}")
    private String bucketName;

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private final DocumentRepository documentRepository;

    private final RestClient restClient;

    private String validateFileType(MultipartFile file) {
        try {
            Tika tika = new Tika();
            String detectedMimeType = tika.detect(file.getInputStream());
            List<String> allowedTypes = List.of(
                    "application/pdf", "image/jpeg", "image/png",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );

            if (!allowedTypes.contains(detectedMimeType)) {
                throw new IllegalArgumentException("Tipo file non consentito o malevolo: " + detectedMimeType);
            }
            return detectedMimeType;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Errore validazione sicurezza file: " + e.getMessage());
        }
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // On supabase "sub" is the unique user ID
            String supabaseUuid = jwtAuth.getTokenAttributes().get("sub").toString();
            return UUID.fromString(supabaseUuid);
        }

        throw new AuthenticationCredentialsNotFoundException("Utente non autenticato o token non valido");
    }

    public String generateSignedUrl(String fileName) {
        if (fileName == null) return null;
        String signUrl = supabaseUrl + "/storage/v1/object/sign/" + bucketName + "/" + fileName;

        Map<String, String> response;
        try {
            Map<String, Object> body = Map.of("expiresIn", 3600); // 1 ora
            response = restClient.post()
                    .uri(signUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new StorageException("Impossibile generare URL firmato");
        }

        if (response == null || response.get("signedURL") == null) {
            throw new StorageException("Risposta non valida da Supabase: signedURL assente");
        }
        return supabaseUrl + response.get("signedURL");
    }

    public List<Document> getDocumentsForUser(UUID userId) {
        UUID callerId = getCurrentUserId();

        if (!callerId.equals(userId) && !isAdmin()) {
            throw new AccessDeniedException("Accesso negato: non puoi visualizzare i documenti di un altro utente.");
        }

        return documentRepository.findByCreatedBy(userId);
    }

    public Document getDocumentByID(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Il documento con ID " + id + " non esiste."));

        if (doc.isSpecial()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                throw new AccessDeniedException("Non hai i permessi per visualizzare questo documento.");
            }
        }
        return doc;
    }

    public List<Document> getExpiringDocumentsReadOnly() {
        LocalDate limitDate = LocalDate.now().plusDays(21);
        List<Document> expiring = documentRepository.findByExpiryDateBefore(limitDate);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return expiring.stream().filter(doc -> !doc.isSpecial()).toList();
        }
        return expiring;
    }

    public List<Document> processAndNotifyExpiringDocuments() {
        LocalDate today = LocalDate.now();
        List<Document> expiringDocs = documentRepository.findByExpiryDateBefore(today.plusDays(21));
        List<Document> toNotify = new ArrayList<>();

        for (Document doc : expiringDocs) {
            if (!doc.isNotified()) {
                toNotify.add(doc);
                doc.setNotified(true);
                doc.setLastNotifiedAt(today);
            } else if (doc.getLastNotifiedAt() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(doc.getLastNotifiedAt(), today);
                if (days >= 11) {
                    toNotify.add(doc);
                    doc.setLastNotifiedAt(today);
                }
            }
        }

        if (!toNotify.isEmpty()) {
            documentRepository.saveAll(toNotify);
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

    public Page<Document> searchAllowedDocuments(String title, String category, LocalDate start, LocalDate end, int page, int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Pageable pageable = PageRequest.of(page, size);

        return documentRepository.searchDocumentsFiltered(title, category, start, end, isAdmin, pageable);
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
        doc.setType(file != null && !file.isEmpty() ? DocumentType.FILE : DocumentType.TEXT_REMINDER);
        doc.setCreatedBy(getCurrentUserId());

        return documentRepository.save(doc);
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

        existingDoc.setExpiryDate(newExpiryDate);
        existingDoc.setNotified(false);
        existingDoc.setLastNotifiedAt(null);
        if (newFile != null && !newFile.isEmpty()) {
            String newFileUrl = uploadFileToSupabase(newFile);
            existingDoc.setFileUrl(newFileUrl);
            existingDoc.setType(DocumentType.FILE);
        }

        return documentRepository.save(existingDoc);
    }

    private String uploadFileToSupabase(MultipartFile file) {

        String correctMimeType = validateFileType(file);

        String cleanName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String safeFileName = Paths.get(cleanName).getFileName().toString();
        String fileName = UUID.randomUUID() + "_" + safeFileName;

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        try {
            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.parseMediaType(correctMimeType))
                    .body(file.getBytes()) // To avoid corruption which we had before
                    .retrieve()
                    .toBodilessEntity();

            return fileName;
        } catch (Exception e) {
            throw new StorageException("Impossibile caricare il file: " + e.getMessage());
        }
    }

    private void deleteFileFromSupabase(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            String[] parts = fileUrl.split("/");
            String fileName = parts[parts.length - 1];

            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName;

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
            log.error("Impossibile eliminare il file da Supabase: {}", e.getMessage(), e);
        }
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
        return doc.getHistory();

        /*I'm leaving this older comment below just for future reference on how the project was built
        But I don't need this anymore since I moved the ordering directly to the query
         */
        /* (old comment)Could also write it like this, which I think is better but, at least to me, less readable
         history.sort(Comparator.comparing(DocumentVersion::getArchivedAt, Comparator.nullsLast(Comparator.reverseOrder())));
         */
    }

    @Transactional
    public void logicalDeleteDocument(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato"));
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDate.now());
        documentRepository.save(doc);
    }

    @Transactional
    public void executePhysicalDeletionForDocument(Document doc) {

        // Gets old and current URLs for the files we want to delete
        List<String> urlsToDelete = new ArrayList<>();
        if (doc.getFileUrl() != null) urlsToDelete.add(doc.getFileUrl());

        doc.getHistory().forEach(version -> {
            if (version.getFileUrl() != null) urlsToDelete.add(version.getFileUrl());
        });

        urlsToDelete.forEach(this::deleteFileFromSupabase);

        documentRepository.delete(doc);
    }
}
