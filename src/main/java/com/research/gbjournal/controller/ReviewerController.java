package com.research.gbjournal.controller;

import com.research.gbjournal.dto.review.SubmitReviewRequest;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviewer")
@RequiredArgsConstructor
public class ReviewerController {

    private final ReviewService reviewService;

    /** GET /api/v1/reviewer/assignments — My assigned manuscripts */
    @GetMapping("/assignments")
    public ResponseEntity<List<SubmissionResponseDTO>> myAssignments(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reviewService.getMyAssignments(userDetails.getUsername()));
    }

    /** POST /api/v1/reviewer/assignments/{id}/accept — Accept invitation */
    @PostMapping("/assignments/{id}/accept")
    public ResponseEntity<Map<String, String>> accept(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        reviewService.respondToInvitation(userDetails.getUsername(), id, true);
        return ResponseEntity.ok(Map.of("message", "Review invitation accepted."));
    }

    /** POST /api/v1/reviewer/assignments/{id}/decline — Decline invitation */
    @PostMapping("/assignments/{id}/decline")
    public ResponseEntity<Map<String, String>> decline(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        reviewService.respondToInvitation(userDetails.getUsername(), id, false);
        return ResponseEntity.ok(Map.of("message", "Review invitation declined."));
    }

    /** POST /api/v1/reviewer/assignments/{id}/submit — Submit completed review */
    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<Map<String, String>> submitReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody SubmitReviewRequest request) {
        reviewService.submitReview(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(Map.of("message", "Review submitted successfully."));
    }

    /** GET /api/v1/reviewer/invitations/{token} — Public invitation overview */
    @GetMapping("/invitations/{token}")
    public ResponseEntity<Map<String, Object>> getInvitation(@PathVariable String token) {
        return ResponseEntity.ok(reviewService.getInvitationByToken(token));
    }

    /** POST /api/v1/reviewer/invitations/{token}/respond — Public token-based accept/decline */
    @PostMapping("/invitations/{token}/respond")
    public ResponseEntity<Map<String, Object>> respondToInvitation(
            @PathVariable String token,
            @RequestParam(required = false, defaultValue = "true") boolean accept) {
        return ResponseEntity.ok(reviewService.respondToInvitationByToken(token, accept));
    }
}
