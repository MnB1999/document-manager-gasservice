package com.azienda.documentmanager.services;

import com.azienda.documentmanager.exceptions.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    // Thread-safe for detection
    private static final Tika TIKA = new Tika();

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf", "image/jpeg", "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final int SIGNED_URL_EXPIRY_SECONDS = 3600; // 1 ora
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private static final int LIST_PAGE_SIZE = 100;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${app.storage.bucket}")
    private String bucketName;

    private final RestClient restClient;

    public String uploadFileToSupabase(MultipartFile file) {
        String correctMimeType = validateFileType(file);
        String fileName = buildStorageFileName(file);
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        try {
            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.parseMediaType(correctMimeType))
                    .body(file.getBytes()) // whole bytes to avoid corruption which we had before
                    .retrieve()
                    .toBodilessEntity();

            return fileName;
        } catch (Exception e) {
            throw new StorageException("Caricamento file su Supabase fallito", e);
        }
    }

    public String generateSignedUrl(String fileName) {
        if (fileName == null) return null;
        String signUrl = supabaseUrl + "/storage/v1/object/sign/" + bucketName + "/" + fileName;

        Map<String, String> response;
        try {
            Map<String, Object> body = Map.of("expiresIn", SIGNED_URL_EXPIRY_SECONDS);
            response = restClient.post()
                    .uri(signUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new StorageException("Impossibile generare URL firmato", e);
        }

        if (response == null || response.get("signedURL") == null) {
            throw new StorageException("Risposta non valida da Supabase: signedURL assente");
        }

        // The API returns a path relative to /storage/v1 (e.g. "/object/sign/bucket/file?token=..."):
        // without this prefix, the final URL points to a non-existent resource (404).
        return supabaseUrl + "/storage/v1" + response.get("signedURL");
    }

    public void deleteFilesFromSupabase(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) return;

        // After SQL migration, file_url contains only the object name
        List<String> cleaned = fileNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .toList();

        if (cleaned.isEmpty()) return;

        try {
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName;
            Map<String, Object> body = Map.of("prefixes", cleaned);

            restClient.method(HttpMethod.DELETE)
                    .uri(deleteUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new StorageException("Eliminazione batch da Supabase fallita", e);
        }
    }

    // Best-effort deletion used when DB rolls back after a successful upload
    public void deleteQuietly(String fileName) {
        try {
            deleteFilesFromSupabase(List.of(fileName));
        } catch (StorageException e) {
            log.warn("Compensazione fallita: file orfano su Supabase: {}", fileName, e);
        }
    }

    private String validateFileType(MultipartFile file) {
        try {
            String detectedMimeType = TIKA.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
                throw new IllegalArgumentException("Tipo file non consentito o malevolo: " + detectedMimeType);
            }
            return detectedMimeType;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Errore validazione sicurezza file", e);
        }
    }

    private String buildStorageFileName(MultipartFile file) {
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        String baseName = Paths.get(StringUtils.cleanPath(original)).getFileName().toString();
        String safe = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.length() > MAX_FILE_NAME_LENGTH) {
            safe = safe.substring(safe.length() - MAX_FILE_NAME_LENGTH); // preserva l'estensione
        }
        if (safe.isBlank() || safe.chars().allMatch(c -> c == '.' || c == '_')) {
            safe = "file";
        }
        return UUID.randomUUID() + "_" + safe;
    }

}