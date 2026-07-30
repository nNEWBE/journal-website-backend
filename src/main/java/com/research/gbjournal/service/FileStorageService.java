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

    /**
     * Safely store an uploaded file using a UUID-based filename.
     *
     * @param file   the multipart file from the HTTP request
     * @param subDir sub-directory within the upload dir (e.g., "submissions")
     * @return the UUID-based stored filename (not the original name)
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing.");
        }

        // Sanitize the original filename to get a safe extension only
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "unknown");
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        try {
            Path targetDir = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFilename).normalize();

            // Path traversal guard — ensure the file is stored inside targetDir
            if (!targetPath.startsWith(targetDir)) {
                throw new BadRequestException("Invalid file path detected.");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file {} -> {}", originalFilename, storedFilename);
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
            Path filePath = Paths.get(uploadDir, subDir).toAbsolutePath().normalize()
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
     * Delete a stored file by UUID filename.
     */
    public void delete(String subDir, String storedFilename) {
        try {
            Path filePath = Paths.get(uploadDir, subDir).toAbsolutePath().normalize()
                    .resolve(storedFilename).normalize();
            Files.deleteIfExists(filePath);
            log.debug("Deleted file: {}", storedFilename);
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
