package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final DocumentRepository documentRepository;

    public List<Document> getDocumentsForUser(UUID userId) {
        return documentRepository.findByCreatedBy(userId);
    }

    public List<Document> checkExpiringDocuments() {
        LocalDate limitDate = LocalDate.now().plusDays(21);
        return documentRepository.findByExpiryDateBeforeAndNotifiedFalse(limitDate);
    }

    public List<Document> getAllAllowedDocuments(String userRole) {
        if ("ADMIN".equals(userRole)) {
            return documentRepository.findAll();
        } else {
            return documentRepository.findBySpecialFalse();
        }
    }

    public Document saveDocument(MultipartFile file, String title, String category, LocalDate expiryDate, boolean isSpecial) {
        String fileUrl = null;

        if (file != null && !file.isEmpty()) {
            fileUrl = uploadFileToSupabase(file);
        }

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

            
            return supabaseUrl + "/storage/v1/object/public/gas-service-docs/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'upload: " + e.getMessage());
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
}
