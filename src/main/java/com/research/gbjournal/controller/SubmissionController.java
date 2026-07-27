package com.research.gbjournal.controller;

import com.research.gbjournal.dto.submission.SubmissionCreateRequest;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /** POST /api/v1/submissions — Create a new draft */
    @PostMapping
    public ResponseEntity<SubmissionResponseDTO> createDraft(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmissionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.createOrUpdateDraft(userDetails.getUsername(), request));
    }

    /** GET /api/v1/submissions/my — List my submissions (author) */
    @GetMapping("/my")
    public ResponseEntity<List<SubmissionResponseDTO>> mySubmissions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(submissionService.getMySubmissions(userDetails.getUsername()));
    }

    /** GET /api/v1/submissions/{id} — Get single submission */
    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponseDTO> getSubmission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmission(userDetails.getUsername(), id));
    }

    /** POST /api/v1/submissions/{id}/submit — Finalise and submit a draft */
    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmissionResponseDTO> submitManuscript(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(submissionService.submitManuscript(userDetails.getUsername(), id));
    }

    /** POST /api/v1/submissions/{id}/files — Upload a file for a submission */
    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponseDTO> uploadFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType) {
        return ResponseEntity.ok(
                submissionService.uploadFile(userDetails.getUsername(), id, file, fileType));
    }

    /** POST /api/v1/submissions/{id}/withdraw — Withdraw a submission */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<SubmissionResponseDTO> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(submissionService.withdrawSubmission(userDetails.getUsername(), id));
    }
}
