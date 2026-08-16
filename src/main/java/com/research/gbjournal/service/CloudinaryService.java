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
}
