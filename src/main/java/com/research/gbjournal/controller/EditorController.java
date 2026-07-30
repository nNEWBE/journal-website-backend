package com.research.gbjournal.controller;

import com.research.gbjournal.dto.editorial.EditorialDecisionRequest;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.service.EditorialService;
import com.research.gbjournal.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/editor")
@RequiredArgsConstructor
public class EditorController {

    private final EditorialService editorialService;
    private final ReviewService reviewService;

    /** GET /api/v1/editor/submissions */
    @GetMapping("/submissions")
    public ResponseEntity<Page<SubmissionResponseDTO>> listSubmissions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(editorialService.listSubmissions(status, type, page, size));
    }

    /** GET /api/v1/editor/submissions/{id} */
    @GetMapping("/submissions/{id}")
    public ResponseEntity<SubmissionResponseDTO> getSubmission(@PathVariable Long id) {
        return ResponseEntity.ok(editorialService.listSubmissions(null, null, 0, 1)
                .stream().findFirst().orElse(null)); // placeholder — see service
    }

    /** POST /api/v1/editor/submissions/{id}/assign-editor */
    @PostMapping("/submissions/{id}/assign-editor")
    public ResponseEntity<SubmissionResponseDTO> assignEditor(
            @PathVariable Long id,
            @RequestParam Long editorId) {
        return ResponseEntity.ok(editorialService.assignEditor(id, editorId));
    }

    /** POST /api/v1/editor/submissions/{id}/assign-reviewer */
    @PostMapping("/submissions/{id}/assign-reviewer")
    public ResponseEntity<Map<String, String>> assignReviewer(
            @PathVariable Long id,
            @RequestParam Long reviewerId,
            @RequestParam(required = false) Instant dueDate) {
        reviewService.assignReviewer(id, reviewerId, dueDate);
        return ResponseEntity.ok(Map.of("message", "Reviewer invited successfully."));
    }

    /** POST /api/v1/editor/submissions/{id}/decision */
    @PostMapping("/submissions/{id}/decision")
    public ResponseEntity<SubmissionResponseDTO> makeDecision(
            @PathVariable Long id,
            @Valid @RequestBody EditorialDecisionRequest request) {
        return ResponseEntity.ok(editorialService.makeDecision(id, request));
    }

    /** POST /api/v1/editor/submissions/{id}/copyediting */
    @PostMapping("/submissions/{id}/copyediting")
    public ResponseEntity<SubmissionResponseDTO> moveToCopyediting(@PathVariable Long id) {
        return ResponseEntity.ok(editorialService.moveToCopyediting(id));
    }

    /** POST /api/v1/editor/submissions/{id}/schedule */
    @PostMapping("/submissions/{id}/schedule")
    public ResponseEntity<SubmissionResponseDTO> scheduleForIssue(@PathVariable Long id) {
        return ResponseEntity.ok(editorialService.scheduleForIssue(id));
    }
}
