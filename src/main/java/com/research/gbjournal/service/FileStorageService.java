package com.research.gbjournal.service;

import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload-dir:uploads/}")
    private String uploadDir;

    @Value("${app.supabase.url:}")
    private String supabaseUrl;

    @Value("${app.supabase.key:}")
    private String supabaseKey;

    @Value("${app.supabase.bucket:manuscripts}")
    private String supabaseBucket;

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

    /**
     * Safely store an uploaded file. If Supabase is configured, uploads directly
     * to Supabase Storage bucket and returns the public URL. Otherwise stores on local disk.
     *
     * @param file   the multipart file from the HTTP request
     * @param subDir sub-directory within the upload dir (e.g., "submissions/SUB-2026-001")
     * @return the public URL or UUID-based stored filename
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing.");
        }

        // Sanitize the original filename to get a safe extension only
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "unknown");
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // 1. Try Supabase Storage if configured
        if (supabaseUrl != null && !supabaseUrl.isBlank() && supabaseKey != null && !supabaseKey.isBlank()) {
            try {
                String cleanSubDir = subDir != null ? subDir.replaceAll("^/+", "").replaceAll("/+$", "") : "";
                String objectPath = cleanSubDir.isEmpty() ? storedFilename : cleanSubDir + "/" + storedFilename;
                String uploadEndpoint = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/" + supabaseBucket + "/" + objectPath;

                String contentType = file.getContentType();
                if (contentType == null || contentType.isBlank()) {
                    contentType = "application/octet-stream";
                }

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(uploadEndpoint))
                        .header("apikey", supabaseKey)
                        .header("Authorization", "Bearer " + supabaseKey)
                        .header("Content-Type", contentType)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    String publicUrl = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/public/" + supabaseBucket + "/" + objectPath;
                    log.info("Uploaded file {} to Supabase Storage: {}", originalFilename, publicUrl);
                    return publicUrl;
                } else {
                    log.warn("Supabase storage upload failed with status {}: {}. Falling back to disk storage.",
                            response.statusCode(), response.body());
                }
            } catch (Exception ex) {
                log.error("Failed to upload to Supabase Storage ({}): {}. Falling back to disk storage.",
                        originalFilename, ex.getMessage());
            }
        }

        // 2. Fallback to local disk storage
        try {
            Path targetDir = Paths.get(uploadDir, subDir != null ? subDir : "").toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFilename).normalize();

            // Path traversal guard — ensure the file is stored inside targetDir
            if (!targetPath.startsWith(targetDir)) {
                throw new BadRequestException("Invalid file path detected.");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file on disk {} -> {}", originalFilename, storedFilename);
            return storedFilename;

        } catch (IOException ex) {
            log.error("Failed to store file {}: {}", originalFilename, ex.getMessage());
            throw new BadRequestException("Could not store file. Please try again.");
        }
    }

    /**
     * Load a stored file as a Spring Resource for download/streaming.
     */
    public Resource loadAsResource(String subDir, String storedFilename) {
        try {
            if (storedFilename != null && (storedFilename.startsWith("http://") || storedFilename.startsWith("https://"))) {
                return new UrlResource(java.net.URI.create(storedFilename));
            }

            Path filePath = Paths.get(uploadDir, subDir != null ? subDir : "").toAbsolutePath().normalize()
                    .resolve(storedFilename).normalize();

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found: " + storedFilename);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + storedFilename);
        }
    }

    /**
     * Delete a stored file by URL or UUID filename.
     */
    public void delete(String subDir, String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            return;
        }

        if (storedFilename.startsWith("http://") || storedFilename.startsWith("https://")) {
            if (supabaseUrl != null && !supabaseUrl.isBlank() && supabaseKey != null && !supabaseKey.isBlank()) {
                try {
                    String publicPrefix = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/public/" + supabaseBucket + "/";
                    if (storedFilename.startsWith(publicPrefix)) {
                        String objectPath = storedFilename.substring(publicPrefix.length());
                        String deleteEndpoint = supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/" + supabaseBucket + "/" + objectPath;

                        java.net.http.HttpRequest delReq = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(deleteEndpoint))
                                .header("apikey", supabaseKey)
                                .header("Authorization", "Bearer " + supabaseKey)
                                .DELETE()
                                .build();
                        httpClient.send(delReq, java.net.http.HttpResponse.BodyHandlers.discarding());
                        log.debug("Deleted file from Supabase Storage: {}", objectPath);
                    }
                } catch (Exception ex) {
                    log.warn("Could not delete file from Supabase Storage {}: {}", storedFilename, ex.getMessage());
                }
            }
            return;
        }

        try {
            Path filePath = Paths.get(uploadDir, subDir != null ? subDir : "").toAbsolutePath().normalize()
                    .resolve(storedFilename).normalize();
            Files.deleteIfExists(filePath);
            log.debug("Deleted file from disk: {}", storedFilename);
        } catch (IOException ex) {
            log.warn("Could not delete file {}: {}", storedFilename, ex.getMessage());
        }
    }

    // ===== Helpers =====

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1)
            return "";
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
