package com.research.gbjournal.service;

import com.research.gbjournal.dto.review.ReviewerPerformanceDTO;
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

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final SubmissionService submissionService;
    private final SubmissionMailService submissionMailService;
    private final NotificationService notificationService;

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

        Submission submission = assignment.getSubmission();
        User reviewer = assignment.getReviewer();

        if (accept) {
            if (submission.getStatus() == Submission.SubmissionStatus.REVIEWER_INVITATION ||
                submission.getStatus() == Submission.SubmissionStatus.WITH_EDITOR) {
                submission.setStatus(Submission.SubmissionStatus.UNDER_REVIEW);
                submissionRepository.save(submission);
            }
            String dueStr = assignment.getDueDate() != null
                    ? DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault()).format(assignment.getDueDate())
                    : "Standard deadline";
            notificationService.createNotification(
                    "Review Invitation Accepted",
                    reviewer.getFullName() + " accepted the review invitation for manuscript " + submission.getSubmissionId() + " (\"" + submission.getTitle() + "\"). Target deadline: " + dueStr + ".",
                    "review",
                    "editor,admin,super_admin",
                    "/dashboard/pipeline"
            );
        } else {
            // Unassign: if no active reviewer remains, revert status to WITH_EDITOR or SUBMITTED
            boolean hasOtherActiveReviewers = reviewAssignmentRepository.findBySubmissionOrderByInvitedAtDesc(submission)
                    .stream()
                    .anyMatch(ra -> !ra.getId().equals(assignment.getId()) &&
                                   (ra.getStatus() == ReviewAssignment.ReviewStatus.INVITED ||
                                    ra.getStatus() == ReviewAssignment.ReviewStatus.ACCEPTED ||
                                    ra.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED));

            if (!hasOtherActiveReviewers) {
                submission.setStatus(submission.getAssignedEditor() != null
                        ? Submission.SubmissionStatus.WITH_EDITOR
                        : Submission.SubmissionStatus.SUBMITTED);
                submissionRepository.save(submission);
            }

            notificationService.createNotification(
                    "Review Invitation Declined",
                    reviewer.getFullName() + " declined the review invitation for manuscript " + submission.getSubmissionId() + " (\"" + submission.getTitle() + "\"). The manuscript is now unassigned.",
                    "review",
                    "editor,admin,super_admin",
                    "/dashboard/pipeline"
            );
        }

        // Async email alert to Super Admins & Admins (and assigned Editor if present)
        try {
            List<User> recipients = new ArrayList<>(
                    userRepository.findByRoleIn(List.of(User.Role.ADMIN, User.Role.SUPER_ADMIN))
            );
            if (submission.getAssignedEditor() != null && !recipients.contains(submission.getAssignedEditor())) {
                recipients.add(submission.getAssignedEditor());
            }
            submissionMailService.sendAdminReviewerResponseNotification(submission, assignment, reviewer, accept, recipients);
        } catch (Exception ex) {
            log.warn("Could not dispatch admin reviewer response notification emails: {}", ex.getMessage());
        }

        log.info("Reviewer {} {} assignment {}", reviewerEmail, accept ? "accepted" : "declined", assignmentId);
    }

    // ===== Reviewer: Submit Review =====

    @Transactional
    public void submitReview(String reviewerEmail, Long assignmentId, SubmitReviewRequest request) {
        ReviewAssignment assignment = getAssignmentOwnedByReviewer(reviewerEmail, assignmentId);

        if (assignment.getStatus() == ReviewAssignment.ReviewStatus.INVITED) {
            assignment.setStatus(ReviewAssignment.ReviewStatus.ACCEPTED);
        } else if (assignment.getStatus() != ReviewAssignment.ReviewStatus.ACCEPTED) {
            if (assignment.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED) {
                throw new BadRequestException("This review has already been submitted.");
            }
            throw new BadRequestException("You must accept the review invitation before submitting a review.");
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

        // Check if all reviews are complete and update reviewScore
        Submission submission = assignment.getSubmission();
        double avgScore = submission.getReviews().stream()
                .filter(r -> r != null && (r.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED || assignment.getId().equals(r.getId())) && r.getScore() != null)
                .mapToInt(r -> r.getScore() != null ? r.getScore() : 0)
                .average()
                .orElse(request.getScore() != null ? request.getScore().doubleValue() : 0.0);

        if (avgScore > 0) {
            submission.setReviewScore((int) Math.round(avgScore));
        }

        boolean allComplete = submission.getReviews().stream()
                .filter(r -> r.getStatus() == ReviewAssignment.ReviewStatus.ACCEPTED ||
                             r.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED)
                .allMatch(r -> r.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED);

        if (allComplete) {
            submission.setStatus(Submission.SubmissionStatus.REVIEWS_COMPLETE);
        }
        submissionRepository.save(submission);

        log.info("Review submitted by {} for submission {}", reviewerEmail, submission.getSubmissionId());
    }

    // ===== Editor: Assign Reviewer =====

    @Transactional
    public void assignReviewer(Long submissionId, Long reviewerId, Instant dueDate) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reviewerId));

        if (reviewer.getRole() != User.Role.REVIEWER
                && reviewer.getRole() != User.Role.EDITOR
                && reviewer.getRole() != User.Role.ADMIN
                && reviewer.getRole() != User.Role.SUPER_ADMIN) {
            throw new BadRequestException("User cannot be assigned as a reviewer.");
        }

        // Prevent duplicate active assignments
        boolean alreadyAssigned = reviewAssignmentRepository.existsBySubmissionAndReviewerAndStatusNot(
                submission, reviewer, ReviewAssignment.ReviewStatus.DECLINED);
        if (alreadyAssigned) {
            throw new BadRequestException("This reviewer is already assigned to this manuscript.");
        }

        String token = UUID.randomUUID().toString();

        ReviewAssignment assignment = ReviewAssignment.builder()
                .submission(submission)
                .reviewer(reviewer)
                .status(ReviewAssignment.ReviewStatus.INVITED)
                .dueDate(dueDate)
                .invitationToken(token)
                .build();

        reviewAssignmentRepository.save(assignment);

        // Async invitation email to reviewer
        submissionMailService.sendReviewerInvitation(assignment);

        if (submission.getStatus() == Submission.SubmissionStatus.WITH_EDITOR ||
            submission.getStatus() == Submission.SubmissionStatus.SUBMITTED) {
            submission.setStatus(Submission.SubmissionStatus.REVIEWER_INVITATION);
            submissionRepository.save(submission);
        }

        log.info("Reviewer {} invited for submission {}", reviewer.getEmail(), submission.getSubmissionId());
    }

    // ===== Remove / Unassign Reviewer =====

    @Transactional
    public void removeReviewer(Long submissionId, Long reviewerId, Long assignmentId, String reviewerName) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));

        List<ReviewAssignment> assignments = reviewAssignmentRepository.findBySubmissionOrderByInvitedAtDesc(submission);
        ReviewAssignment targetAssignment = null;

        if (assignmentId != null) {
            targetAssignment = assignments.stream()
                    .filter(ra -> ra.getId().equals(assignmentId))
                    .findFirst().orElse(null);
        }

        if (targetAssignment == null && reviewerId != null) {
            targetAssignment = assignments.stream()
                    .filter(ra -> ra.getReviewer().getId().equals(reviewerId) &&
                            (ra.getStatus() == ReviewAssignment.ReviewStatus.INVITED ||
                             ra.getStatus() == ReviewAssignment.ReviewStatus.ACCEPTED))
                    .findFirst().orElse(null);
        }

        if (targetAssignment == null && reviewerName != null && !reviewerName.isBlank()) {
            String targetClean = reviewerName.trim().toLowerCase();
            targetAssignment = assignments.stream()
                    .filter(ra -> (ra.getReviewer().getFullName().trim().equalsIgnoreCase(targetClean) ||
                                   ra.getReviewer().getEmail().trim().equalsIgnoreCase(targetClean)) &&
                            (ra.getStatus() == ReviewAssignment.ReviewStatus.INVITED ||
                             ra.getStatus() == ReviewAssignment.ReviewStatus.ACCEPTED))
                    .findFirst().orElse(null);
        }

        if (targetAssignment == null) {
            throw new ResourceNotFoundException("Active review assignment not found for the specified referee.");
        }

        User reviewer = targetAssignment.getReviewer();
        final Long removedAssignmentId = targetAssignment.getId();

        // Delete the assignment
        reviewAssignmentRepository.delete(targetAssignment);

        // Check if other active reviewers remain on the submission
        boolean hasRemainingActiveReviewers = assignments.stream()
                .anyMatch(ra -> !ra.getId().equals(removedAssignmentId) &&
                               (ra.getStatus() == ReviewAssignment.ReviewStatus.INVITED ||
                                ra.getStatus() == ReviewAssignment.ReviewStatus.ACCEPTED ||
                                ra.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED));

        if (!hasRemainingActiveReviewers) {
            if (submission.getStatus() == Submission.SubmissionStatus.UNDER_REVIEW ||
                submission.getStatus() == Submission.SubmissionStatus.REVIEWER_INVITATION) {
                submission.setStatus(submission.getAssignedEditor() != null
                        ? Submission.SubmissionStatus.WITH_EDITOR
                        : Submission.SubmissionStatus.SUBMITTED);
                submissionRepository.save(submission);
            }
        }

        notificationService.createNotification(
                "Reviewer Unassigned",
                reviewer.getFullName() + " was unassigned from manuscript " + submission.getSubmissionId() + " (\"" + submission.getTitle() + "\").",
                "review",
                "editor,admin,super_admin",
                "/dashboard/pipeline"
        );

        // Dispatch email notification to unassigned referee
        submissionMailService.sendReviewerUnassignedNotification(submission, reviewer);

        log.info("Reviewer {} unassigned from submission {}", reviewer.getEmail(), submission.getSubmissionId());
    }

    // ===== Token-based Invitation Response (from Email) =====

    @Transactional(readOnly = true)
    public Map<String, Object> getInvitationByToken(String token) {
        ReviewAssignment assignment = reviewAssignmentRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Review assignment not found or token expired."));
        Submission sub = assignment.getSubmission();
        return Map.of(
                "assignmentId", assignment.getId(),
                "status", assignment.getStatus().name(),
                "submissionId", sub.getSubmissionId(),
                "title", sub.getTitle(),
                "type", sub.getType() != null ? sub.getType() : "Research Article",
                "topic", sub.getTopic() != null ? sub.getTopic() : "",
                "abstractText", sub.getAbstractText() != null ? sub.getAbstractText() : "",
                "reviewerName", assignment.getReviewer().getFullName(),
                "reviewerEmail", assignment.getReviewer().getEmail(),
                "dueDate", assignment.getDueDate() != null ? assignment.getDueDate().toString() : ""
        );
    }

    @Transactional
    public Map<String, Object> respondToInvitationByToken(String token, boolean accept) {
        ReviewAssignment assignment = reviewAssignmentRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Review assignment not found or token expired."));

        if (assignment.getStatus() != ReviewAssignment.ReviewStatus.INVITED) {
            return Map.of(
                    "alreadyResponded", true,
                    "status", assignment.getStatus().name(),
                    "submissionId", assignment.getSubmission().getSubmissionId(),
                    "reviewerName", assignment.getReviewer().getFullName(),
                    "title", assignment.getSubmission().getTitle()
            );
        }

        respondToInvitation(assignment.getReviewer().getEmail(), assignment.getId(), accept);

        return Map.of(
                "success", true,
                "action", accept ? "accepted" : "declined",
                "status", assignment.getStatus().name(),
                "submissionId", assignment.getSubmission().getSubmissionId(),
                "reviewerName", assignment.getReviewer().getFullName(),
                "title", assignment.getSubmission().getTitle(),
                "dueDate", assignment.getDueDate() != null ? assignment.getDueDate().toString() : ""
        );
    }

    // ===== Helpers =====

    private User getReviewer(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ReviewAssignment getAssignmentOwnedByReviewer(String reviewerEmail, Long id) {
        User reviewer = getReviewer(reviewerEmail);
        return reviewAssignmentRepository.findById(id)
                .filter(ra -> ra.getReviewer().getId().equals(reviewer.getId()))
                .or(() -> reviewAssignmentRepository.findBySubmissionIdAndReviewer(id, reviewer).stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Review assignment not found for reviewer " + reviewerEmail));
    }

    // ===== Reviewer Performance & Capacity Analytics =====

    @Transactional(readOnly = true)
    public ReviewerPerformanceDTO getReviewerPerformance(Long reviewerId) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reviewerId));

        List<ReviewAssignment> allAssignments = reviewAssignmentRepository.findByReviewerOrderByInvitedAtDesc(reviewer);

        int maxCapacity = 3;
        int activeReviews = 0;
        int completedReviews = 0;
        int totalInvitations = allAssignments.size();
        int onTimeCompletedCount = 0;
        double totalTurnaroundDays = 0;
        int scoredReviewsCount = 0;
        double totalScore = 0;

        List<ReviewerPerformanceDTO.TurnaroundItemDTO> turnaroundHistory = new ArrayList<>();

        for (ReviewAssignment ra : allAssignments) {
            if (ra.getStatus() == ReviewAssignment.ReviewStatus.INVITED ||
                ra.getStatus() == ReviewAssignment.ReviewStatus.ACCEPTED) {
                activeReviews++;
            } else if (ra.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED) {
                completedReviews++;

                // Calculate turnaround days
                Instant startTime = ra.getInvitedAt();
                Instant endTime = ra.getReviewSubmittedAt() != null ? ra.getReviewSubmittedAt() : Instant.now();
                double days = 0.0;
                if (startTime != null && ra.getReviewSubmittedAt() != null) {
                    long diffHours = Duration.between(startTime, endTime).toHours();
                    days = Math.max(0.5, Math.round((diffHours / 24.0) * 10.0) / 10.0);
                }
                totalTurnaroundDays += days;

                // Check on-time compliance
                boolean isOnTime = true;
                int targetDays = 14;
                if (ra.getDueDate() != null && startTime != null) {
                    targetDays = Math.max(1, (int) Duration.between(startTime, ra.getDueDate()).toDays());
                }
                if (ra.getDueDate() != null && ra.getReviewSubmittedAt() != null) {
                    isOnTime = !ra.getReviewSubmittedAt().isAfter(ra.getDueDate());
                }
                if (isOnTime) {
                    onTimeCompletedCount++;
                }

                if (ra.getScore() != null) {
                    totalScore += ra.getScore();
                    scoredReviewsCount++;
                }

                // Add up to 5 most recent completed reviews to turnaroundHistory
                if (turnaroundHistory.size() < 5) {
                    String paperId = (ra.getSubmission() != null && ra.getSubmission().getSubmissionId() != null)
                            ? ra.getSubmission().getSubmissionId()
                            : ("MS-" + ra.getId());
                    int diff = (int) Math.round(targetDays - days);
                    String variance = diff > 0 ? (diff + "d ahead") : (diff < 0 ? (Math.abs(diff) + "d overdue") : "On schedule");
                    turnaroundHistory.add(ReviewerPerformanceDTO.TurnaroundItemDTO.builder()
                            .paper(paperId)
                            .days(days)
                            .target(targetDays)
                            .variance(variance)
                            .build());
                }
            }
        }

        Double onTimeTargetRate = completedReviews > 0
                ? Math.round((onTimeCompletedCount * 100.0) / completedReviews * 10.0) / 10.0
                : null;

        Double avgTurnaroundDays = completedReviews > 0
                ? Math.round((totalTurnaroundDays / completedReviews) * 10.0) / 10.0
                : null;

        Double rating = scoredReviewsCount > 0
                ? Math.round(((totalScore / scoredReviewsCount) / 20.0) * 10.0) / 10.0
                : null;

        String stressLevel = "Low Stress";
        String stressVariant = "emerald";
        if (activeReviews >= maxCapacity) {
            stressLevel = "High Load";
            stressVariant = "rose";
        } else if (activeReviews == 2) {
            stressLevel = "Moderate";
            stressVariant = "amber";
        }

        String currentActivityText;
        if (activeReviews == 0) {
            currentActivityText = "Fully Available \u2022 No active reviews in queue";
        } else if (activeReviews == 1) {
            currentActivityText = "1 Active Manuscript in progress (On schedule for deadline)";
        } else {
            currentActivityText = activeReviews + " Active Manuscripts in queue (" + stressLevel + ")";
        }

        // Monthly activity for last 5 months
        List<ReviewerPerformanceDTO.MonthlyActivityDTO> monthlyActivity = new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.now();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = 4; i >= 0; i--) {
            YearMonth ym = currentYearMonth.minusMonths(i);
            String monthName = ym.format(monthFormatter);
            int mCompleted = 0;
            int mOnTime = 0;

            for (ReviewAssignment ra : allAssignments) {
                if (ra.getStatus() == ReviewAssignment.ReviewStatus.COMPLETED && ra.getReviewSubmittedAt() != null) {
                    YearMonth reviewYm = YearMonth.from(ra.getReviewSubmittedAt().atZone(ZoneId.systemDefault()));
                    if (reviewYm.equals(ym)) {
                        mCompleted++;
                        if (ra.getDueDate() == null || !ra.getReviewSubmittedAt().isAfter(ra.getDueDate())) {
                            mOnTime++;
                        }
                    }
                }
            }

            monthlyActivity.add(ReviewerPerformanceDTO.MonthlyActivityDTO.builder()
                    .month(monthName)
                    .completed(mCompleted)
                    .onTime(mOnTime)
                    .build());
        }

        return ReviewerPerformanceDTO.builder()
                .reviewerId(reviewer.getId())
                .reviewerName(reviewer.getFullName())
                .email(reviewer.getEmail())
                .activeReviews(activeReviews)
                .completedReviews(completedReviews)
                .totalInvitations(totalInvitations)
                .maxCapacity(maxCapacity)
                .onTimeTargetRate(onTimeTargetRate)
                .avgTurnaroundDays(avgTurnaroundDays)
                .rating(rating)
                .stressLevel(stressLevel)
                .stressVariant(stressVariant)
                .currentActivityText(currentActivityText)
                .turnaroundHistory(turnaroundHistory)
                .monthlyActivity(monthlyActivity)
                .build();
    }
}
