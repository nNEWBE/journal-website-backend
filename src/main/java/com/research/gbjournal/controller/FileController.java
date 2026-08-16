package com.research.gbjournal.controller;

import com.research.gbjournal.entity.SubmissionFile;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.SubmissionFileRepository;
import com.research.gbjournal.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final SubmissionFileRepository submissionFileRepository;
    private final com.research.gbjournal.service.CloudinaryService cloudinaryService;

    /**
     * POST /api/v1/files/upload-image
     * Upload an image to Cloudinary (for cover images, author avatars, manuscript figures).
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<java.util.Map<String, Object>> uploadImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "gbjournal/images") String folder) {
        java.util.Map<String, Object> result = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(java.util.Map.of(
                "url", result.get("secure_url"),
                "publicId", result.get("public_id"),
                "format", result.get("format"),
                "width", result.get("width"),
                "height", result.get("height")
        ));
    }

    /**
     * GET /api/v1/files/{storedFilename}
     * Serves a stored submission file.
     */
    @GetMapping("/{storedFilename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String storedFilename) {
        SubmissionFile metadata = submissionFileRepository.findByStoredFilename(storedFilename)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));

        // Derive the sub-directory from the stored filename pattern (UUID-based)
        String subDir = "submissions/" + metadata.getSubmission().getSubmissionId();
        Resource resource = fileStorageService.loadAsResource(subDir, storedFilename);

        String contentType = metadata.getContentType() != null
                ? metadata.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }
}
