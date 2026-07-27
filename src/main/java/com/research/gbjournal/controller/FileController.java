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

    /**
     * GET /api/v1/files/{storedFilename}
     * Serves a stored submission file. Protected by submission ownership in the service layer;
     * here we keep it simple and serve by UUID filename (not guessable).
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
