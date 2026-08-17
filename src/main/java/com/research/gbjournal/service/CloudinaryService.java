package com.research.gbjournal.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.research.gbjournal.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload an image file to Cloudinary with automatic optimization.
     *
     * @param file   MultipartFile uploaded from client
     * @param folder Cloudinary folder (e.g. "gbjournal/covers", "gbjournal/manuscripts")
     * @return Map containing secure_url, public_id, format, etc.
     */
    public Map<String, Object> uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No image file provided for upload.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File must be an image (JPEG, PNG, WebP, etc.). Provided: " + contentType);
        }

        try {
            String targetFolder = (folder != null && !folder.isBlank()) ? folder.trim() : "gbjournal/images";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", targetFolder,
                            "resource_type", "image",
                            "overwrite", false
                    )
            );

            log.info("Successfully uploaded image to Cloudinary: url={}, public_id={}",
                    uploadResult.get("secure_url"), uploadResult.get("public_id"));

            return uploadResult;
        } catch (IOException ex) {
            log.error("Cloudinary upload IO failure", ex);
            throw new BadRequestException("Failed to upload image to Cloudinary: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Cloudinary upload failed", ex);
            throw new BadRequestException("Cloudinary upload error: " + ex.getMessage());
        }
    }

    /**
     * Upload any generic file (e.g., PDF, manuscript supplement)
     */
    public Map<String, Object> uploadFile(MultipartFile file, String folder, String resourceType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided for upload.");
        }

        try {
            String targetFolder = (folder != null && !folder.isBlank()) ? folder.trim() : "gbjournal/documents";
            String resType = (resourceType != null && !resourceType.isBlank()) ? resourceType : "auto";

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", targetFolder,
                            "resource_type", resType,
                            "overwrite", false
                    )
            );

            log.info("Successfully uploaded file to Cloudinary: url={}, public_id={}",
                    uploadResult.get("secure_url"), uploadResult.get("public_id"));

            return uploadResult;
        } catch (IOException ex) {
            log.error("Cloudinary file upload IO failure", ex);
            throw new BadRequestException("Failed to upload file to Cloudinary: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Cloudinary file upload failed", ex);
            throw new BadRequestException("Cloudinary file upload error: " + ex.getMessage());
        }
    }

    /**
     * Delete an asset from Cloudinary by public ID.
     */
    public Map<String, Object> deleteFile(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new BadRequestException("Cloudinary publicId cannot be blank.");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().destroy(
                    publicId, ObjectUtils.emptyMap()
            );
            log.info("Deleted asset from Cloudinary: publicId={}", publicId);
            return result;
        } catch (Exception ex) {
            log.error("Failed to delete asset from Cloudinary: publicId={}", publicId, ex);
            throw new BadRequestException("Cloudinary delete error: " + ex.getMessage());
        }
    }

    /**
     * Safely delete an existing asset from Cloudinary using its full URL.
     * Silently logs if the URL is not a Cloudinary asset or if deletion fails.
     */
    public void deleteByUrl(String url) {
        if (url == null || !url.contains("cloudinary.com")) {
            return;
        }

        String publicId = extractPublicIdFromUrl(url);
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().destroy(
                    publicId, ObjectUtils.emptyMap()
            );
            log.info("Deleted previous Cloudinary avatar: publicId={}, result={}", publicId, result);
        } catch (Exception ex) {
            log.warn("Could not delete old avatar from Cloudinary (publicId={}): {}", publicId, ex.getMessage());
        }
    }

    /**
     * Extracts public_id from a Cloudinary image URL.
     * E.g. https://res.cloudinary.com/demo/image/upload/v12345/gbjournal/avatars/user123.jpg -> gbjournal/avatars/user123
     */
    public String extractPublicIdFromUrl(String url) {
        if (url == null || !url.contains("/upload/")) {
            return null;
        }

        try {
            String afterUpload = url.substring(url.indexOf("/upload/") + "/upload/".length());
            // Strip version if present (e.g. v1700000000/)
            if (afterUpload.matches("^v\\d+/.*")) {
                afterUpload = afterUpload.replaceFirst("^v\\d+/", "");
            }

            // Strip query params if any
            if (afterUpload.contains("?")) {
                afterUpload = afterUpload.substring(0, afterUpload.indexOf("?"));
            }

            // Strip extension (.jpg, .png, etc.)
            int lastDot = afterUpload.lastIndexOf('.');
            if (lastDot > 0) {
                return afterUpload.substring(0, lastDot);
            }
            return afterUpload;
        } catch (Exception ex) {
            log.warn("Failed to extract public_id from URL: {}", url);
            return null;
        }
    }
}
