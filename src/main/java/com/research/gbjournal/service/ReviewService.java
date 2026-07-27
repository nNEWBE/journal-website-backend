package com.research.gbjournal.service;

import com.research.gbjournal.dto.review.SubmitReviewRequest;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.entity.*;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.ReviewAssignmentRepository;
import com.research.gbjournal.repository.SubmissionRepository;
import com.research.gbjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final SubmissionService submissionService;

    // ===== Reviewer: Get My Assignments =====

    @Transactional(readOnly = true)
    public List<SubmissionResponseDTO> getMyAssignments(String reviewerEmail) {
        User reviewer = getReviewer(reviewerEmail);
        return reviewAssignmentRepository.findByReviewerOrderByInvitedAtDesc(reviewer)
                .stream()
                .map(ra -> submissionService.toResponseDTO(ra.getSubmission()))
                .toList();
    }

    // ===== Reviewer: Respond to Invitation =====

    @Transactional
    public void respondToInvitation(String reviewerEmail, Long assignmentId, boolean accept) {
        ReviewAssignment assignment = getAssignmentOwnedByReviewer(reviewerEmail, assignmentId);

        if (assignment.getStatus() != ReviewAssignment.ReviewStatus.INVITED) {
            throw new BadRequestException("You have already responded to this invitation.");
        }

        assignment.setStatus(accept ? ReviewAssignment.ReviewStatus.ACCEPTED : ReviewAssignment.ReviewStatus.DECLINED);
        reviewAssignmentRepository.save(assignment);
        log.info("Reviewer {} {} assignment {}", reviewerEmail, accept ? "accepted" : "declined", assignmentId);
    }

    // ===== Reviewer: Submit Review =====

    @Transactional
    public void submitReview(String reviewerEmail, Long assignmentId, SubmitReviewRequest request) {
        ReviewAssignment assignment = getAssignmentOwnedByReviewer(reviewerEmail, assignmentId);

        if (assignment.getStatus() != ReviewAssignment.ReviewStatus.ACCEPTED) {
            throw new BadRequestException("You must accept the review invitation before submitting a review.");
        }
        if (assignment.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED) {
            throw new BadRequestException("This review has already been submitted.");
        }

        ReviewAssignment.ReviewRecommendation recommendation;
        try {
            recommendation = ReviewAssignment.ReviewRecommendation.valueOf(request.getRecommendation().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid recommendation value: " + request.getRecommendation());
        }

        assignment.setReviewComments(request.getReviewComments());
        assignment.setConfidentialComments(request.getConfidentialComments());
        assignment.setRecommendation(recommendation);
        assignment.setScore(request.getScore());
        assignment.setStatus(ReviewAssignment.ReviewStatus.COMPLETED);
        assignment.setReviewSubmittedAt(Instant.now());
        reviewAssignmentRepository.save(assignment);

        // Check if all reviews are complete — update submission status
        Submission submission = assignment.getSubmission();
        boolean allComplete = submission.getReviews().stream()
                .filter(r -> r.getStatus() == ReviewAssignment.ReviewStatus.ACCEPTED ||
                             r.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED)
                .allMatch(r -> r.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED);

        if (allComplete) {
            submission.setStatus(Submission.SubmissionStatus.REVIEWS_COMPLETE);
            submissionRepository.save(submission);
        }

        log.info("Review submitted by {} for submission {}", reviewerEmail, submission.getSubmissionId());
    }

    // ===== Editor: Assign Reviewer =====

    @Transactional
    public void assignReviewer(Long submissionId, Long reviewerId, Instant dueDate) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reviewerId));

        if (reviewer.getRole() != User.Role.REVIEWER) {
            throw new BadRequestException("User is not a reviewer.");
        }

        // Prevent duplicate active assignments
        boolean alreadyAssigned = reviewAssignmentRepository.existsBySubmissionAndReviewerAndStatusNot(
                submission, reviewer, ReviewAssignment.ReviewStatus.DECLINED);
        if (alreadyAssigned) {
            throw new BadRequestException("This reviewer is already assigned to this manuscript.");
        }

        ReviewAssignment assignment = ReviewAssignment.builder()
                .submission(submission)
                .reviewer(reviewer)
                .status(ReviewAssignment.ReviewStatus.INVITED)
                .dueDate(dueDate)
                .build();

        reviewAssignmentRepository.save(assignment);

        if (submission.getStatus() == Submission.SubmissionStatus.WITH_EDITOR ||
            submission.getStatus() == Submission.SubmissionStatus.SUBMITTED) {
            submission.setStatus(Submission.SubmissionStatus.REVIEWER_INVITATION);
            submissionRepository.save(submission);
        }

        log.info("Reviewer {} invited for submission {}", reviewer.getEmail(), submission.getSubmissionId());
    }

    // ===== Helpers =====

    private User getReviewer(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ReviewAssignment getAssignmentOwnedByReviewer(String reviewerEmail, Long assignmentId) {
        User reviewer = getReviewer(reviewerEmail);
        ReviewAssignment assignment = reviewAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewAssignment", "id", assignmentId));
        if (!assignment.getReviewer().getId().equals(reviewer.getId())) {
            throw new BadRequestException("You are not authorized to access this review assignment.");
        }
        return assignment;
    }
}
