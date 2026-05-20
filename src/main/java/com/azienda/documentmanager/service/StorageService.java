package com.azienda.documentmanager.service;

import com.azienda.documentmanager.exception.StorageException;
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
public class StorageService{

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${app.storage.bucket}")
    private String bucketName;

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

    public String uploadFileToSupabase(MultipartFile file) {

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

    public void deleteFileFromSupabase(String fileUrl) {
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

}
