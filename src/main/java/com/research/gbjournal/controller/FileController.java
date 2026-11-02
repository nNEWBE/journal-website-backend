package com.research.gbjournal.controller;

import com.research.gbjournal.entity.Submission;
import com.research.gbjournal.entity.SubmissionFile;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.ArticleRepository;
import com.research.gbjournal.repository.ReviewAssignmentRepository;
import com.research.gbjournal.repository.SubmissionFileRepository;
import com.research.gbjournal.repository.UserRepository;
import com.research.gbjournal.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    /** Cloudinary folders the upload endpoint may write to: "gbjournal/<name>". */
    private static final Pattern ALLOWED_IMAGE_FOLDER = Pattern.compile("^gbjournal/[a-z0-9_-]{1,40}$");

    /** Content types that are safe to render inline in a browser. Everything else downloads. */
    private static final Set<String> INLINE_SAFE_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp");

    private final FileStorageService fileStorageService;
    private final SubmissionFileRepository submissionFileRepository;
    private final ArticleRepository articleRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final UserRepository userRepository;
    private final com.research.gbjournal.service.CloudinaryService cloudinaryService;

    /**
     * POST /api/v1/files/upload-image
     * Upload an image to Cloudinary (for cover images, author avatars, manuscript figures).
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<java.util.Map<String, Object>> uploadImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "gbjournal/images") String folder) {
        if (!ALLOWED_IMAGE_FOLDER.matcher(folder.trim()).matches()) {
            throw new BadRequestException("Invalid upload folder.");
        }
        java.util.Map<String, Object> result = cloudinaryService.uploadImage(file, folder.trim());
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
     * Serves a stored submission file for inline preview or download.
     *
     * The PDF of a published article is public. Every other manuscript file is only available
     * to its author, editors/administrators, and reviewers assigned to the submission.
     */
    @GetMapping("/{storedFilename}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Resource> serveFile(
            @PathVariable String storedFilename,
            @AuthenticationPrincipal UserDetails userDetails) {
        SubmissionFile metadata = submissionFileRepository.findByStoredFilename(storedFilename)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));

        Submission submission = metadata.getSubmission();
        if (!isPublishedArticleFile(storedFilename) && !canAccessSubmission(metadata, userDetails)) {
            // Anonymous callers get a 404 so private file names cannot be probed
            if (userDetails == null) {
                throw new ResourceNotFoundException("File not found.");
            }
            throw new AccessDeniedException("You do not have access to this file.");
        }

        // Derive the sub-directory from the stored filename pattern (UUID-based)
        String subDir = "submissions/" + submission.getSubmissionId();
        Resource resource = fileStorageService.loadAsResource(subDir, storedFilename);

        String contentType = metadata.getContentType();
        if (contentType == null || contentType.isBlank() || contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            if (metadata.getOriginalFilename() != null && metadata.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                contentType = MediaType.APPLICATION_PDF_VALUE;
            } else {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        // Uploaded HTML/SVG/etc. must never render in the browser (stored XSS), so only
        // known-safe types are shown inline; everything else is a plain download.
        boolean inline = INLINE_SAFE_TYPES.contains(mediaType.getType() + "/" + mediaType.getSubtype());
        if (!inline) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String filename = metadata.getOriginalFilename() != null ? metadata.getOriginalFilename() : storedFilename;
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private boolean isPublishedArticleFile(String storedFilename) {
        return articleRepository.existsByPdfUrl("/api/v1/files/" + storedFilename);
    }

    private boolean canAccessSubmission(SubmissionFile file, UserDetails userDetails) {
        Submission submission = file.getSubmission();
        if (userDetails == null) {
            return false;
        }
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.getRole() == User.Role.EDITOR
                || user.getRole() == User.Role.ADMIN
                || user.getRole() == User.Role.SUPER_ADMIN) {
            return true;
        }
        if (submission.getSubmittingAuthor() != null
                && submission.getSubmittingAuthor().getId().equals(user.getId())) {
            return true;
        }
        return !file.getFileType().revealsAuthorIdentity()
                && reviewAssignmentRepository.existsBySubmissionAndReviewer(submission, user);
    }
}
