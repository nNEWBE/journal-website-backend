package com.research.gbjournal.service;

import com.research.gbjournal.config.MailProperties;
import com.research.gbjournal.entity.ReviewAssignment;
import com.research.gbjournal.entity.Submission;
import com.research.gbjournal.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business-level email triggers for the submission workflow.
 *
 * <p>Each method assembles the template context and delegates to {@link EmailService},
 * which handles the async sending and SMTP error fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionMailService {

    private final EmailService emailService;
    private final MailProperties mailProperties;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneId.of("Asia/Dhaka"));

    // =========================================================
    // 1. SUBMISSION CONFIRMATION — Author → Just submitted
    // =========================================================

    /**
     * Sent immediately when an author transitions their manuscript from DRAFT → SUBMITTED.
     */
    public void sendSubmissionConfirmation(Submission submission) {
        String authorEmail = submission.getSubmittingAuthor().getEmail();
        String authorName  = submission.getSubmittingAuthor().getFullName();

        Map<String, Object> vars = new HashMap<>();
        vars.put("authorName",     authorName);
        vars.put("submissionId",   submission.getSubmissionId());
        vars.put("title",          submission.getTitle());
        vars.put("articleType",    submission.getType());
        vars.put("submittedAt",    DATE_FMT.format(
                submission.getSubmittedAt() != null ? submission.getSubmittedAt() : Instant.now()));
        vars.put("submissionUrl",  mailProperties.getJournalUrl()
                + "/dashboard/author/manuscripts/" + submission.getId());

        emailService.sendHtml(
                authorEmail,
                "GBJ — Manuscript Received: " + submission.getSubmissionId(),
                "email/submission-confirmation",
                vars);

        log.debug("Submission confirmation queued for {} ({})", authorEmail, submission.getSubmissionId());
    }

    /**
     * Sent immediately to Super Admins and Admins whenever any author submits a new manuscript.
     */
    public void sendAdminNewSubmissionNotification(Submission submission, List<User> admins) {
        if (admins == null || admins.isEmpty()) return;

        String authorEmail = submission.getSubmittingAuthor().getEmail();
        String authorName  = submission.getSubmittingAuthor().getFullName();
        int authorCount    = submission.getAuthors() != null ? submission.getAuthors().size() : 1;

        for (User admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) continue;

            Map<String, Object> vars = new HashMap<>();
            vars.put("adminName",    admin.getFullName() != null ? admin.getFullName() : "Administrator");
            vars.put("submissionId", submission.getSubmissionId());
            vars.put("title",        submission.getTitle());
            vars.put("articleType",  submission.getType() != null ? submission.getType() : "Research Article");
            vars.put("authorName",   authorName);
            vars.put("authorEmail",  authorEmail);
            vars.put("authorCount",  authorCount);
            vars.put("submittedAt",  DATE_FMT.format(
                    submission.getSubmittedAt() != null ? submission.getSubmittedAt() : Instant.now()));
            vars.put("pipelineUrl",  mailProperties.getJournalUrl() + "/dashboard/pipeline");

            emailService.sendHtml(
                    admin.getEmail(),
                    "GBJ Editorial Alert: New Manuscript Submitted — " + submission.getSubmissionId(),
                    "email/admin-new-submission",
                    vars);

            log.info("Admin new submission email queued for admin {} ({})", admin.getEmail(), submission.getSubmissionId());
        }
    }

    // =========================================================
    // 2. EDITORIAL DECISION — Author → Status changed by admin/editor
    // =========================================================

    /**
     * Sent whenever an editor changes the submission status to ACCEPTED, REJECTED,
     * REVISION_REQUESTED, SCHEDULED, or PUBLISHED.
     */
    public void sendStatusChangeNotification(Submission submission) {
        String authorEmail = submission.getSubmittingAuthor().getEmail();
        String authorName  = submission.getSubmittingAuthor().getFullName();

        DecisionTexts texts = buildDecisionTexts(submission.getStatus());

        Map<String, Object> vars = new HashMap<>();
        vars.put("authorName",    authorName);
        vars.put("submissionId",  submission.getSubmissionId());
        vars.put("title",         submission.getTitle());
        vars.put("decisionDate",  DATE_FMT.format(
                submission.getDecisionDate() != null ? submission.getDecisionDate() : Instant.now()));
        vars.put("decisionLabel", texts.label);
        vars.put("headerClass",   texts.headerClass);
        vars.put("chipClass",     texts.chipClass);
        vars.put("introText",     texts.intro);
        vars.put("closingText",   texts.closing);
        vars.put("editorNote",    submission.getEditorDecisionNote());
        vars.put("submissionUrl", mailProperties.getJournalUrl()
                + "/dashboard/author/manuscripts/" + submission.getId());

        emailService.sendHtml(
                authorEmail,
                "GBJ — Editorial Decision: " + texts.label + " — " + submission.getSubmissionId(),
                "email/editorial-decision",
                vars);

        log.debug("Status-change email ({}) queued for {} ({})",
                texts.label, authorEmail, submission.getSubmissionId());
    }

    // =========================================================
    // 3. REVIEWER INVITATION — Reviewer → Invited to review
    // =========================================================

    /**
     * Sent when an editor assigns a new peer reviewer to a manuscript.
     */
    public void sendReviewerInvitation(ReviewAssignment assignment) {
        String reviewerEmail = assignment.getReviewer().getEmail();
        String reviewerName  = assignment.getReviewer().getFullName();
        Submission submission = assignment.getSubmission();

        String token = assignment.getInvitationToken() != null ? assignment.getInvitationToken() : assignment.getId().toString();
        String invitationBase = mailProperties.getJournalUrl() + "/review-invitation?token=" + token;

        Map<String, Object> vars = new HashMap<>();
        vars.put("reviewerName",  reviewerName);
        vars.put("submissionId",  submission.getSubmissionId());
        vars.put("title",         submission.getTitle());
        vars.put("articleType",   submission.getType());
        vars.put("dueDate",       assignment.getDueDate() != null
                ? DATE_FMT.format(assignment.getDueDate()) : "14 days from invitation");
        vars.put("acceptUrl",  invitationBase + "&action=accept");
        vars.put("declineUrl", invitationBase + "&action=decline");
        vars.put("journalUrl", mailProperties.getJournalUrl());

        emailService.sendHtml(
                reviewerEmail,
                "GBJ — Review Invitation: " + submission.getSubmissionId(),
                "email/reviewer-invitation",
                vars);

        log.debug("Reviewer invitation queued for {} ({})", reviewerEmail, submission.getSubmissionId());
    }

    // =========================================================
    // 4. WITHDRAWAL CONFIRMATION — Author → Paper withdrawn
    // =========================================================

    /**
     * Sent when an author withdraws their own manuscript.
     */
    public void sendWithdrawalConfirmation(Submission submission) {
        String authorEmail = submission.getSubmittingAuthor().getEmail();
        String authorName  = submission.getSubmittingAuthor().getFullName();

        Map<String, Object> vars = new HashMap<>();
        vars.put("authorName",    authorName);
        vars.put("submissionId",  submission.getSubmissionId());
        vars.put("title",         submission.getTitle());
        vars.put("decisionDate",  DATE_FMT.format(Instant.now()));
        vars.put("decisionLabel", "Withdrawn");
        vars.put("headerClass",   "header-default");
        vars.put("chipClass",     "chip-default");
        vars.put("introText",
                "This is to confirm that you have successfully withdrawn your manuscript from the "
                + "Gono Bishwabidyalay Journal. The submission has been closed and will not proceed "
                + "to further review.");
        vars.put("closingText",
                "If you believe this was a mistake, please contact our editorial office. "
                + "You are welcome to submit a revised version at any time.");
        vars.put("editorNote", null);
        vars.put("submissionUrl", mailProperties.getJournalUrl() + "/dashboard/author/manuscripts");

        emailService.sendHtml(
                authorEmail,
                "GBJ — Manuscript Withdrawn: " + submission.getSubmissionId(),
                "email/editorial-decision",
                vars);
    }

    // =========================================================
    // 5. REVIEWER RESPONSE ALERT — Super Admins & Admins → Accepted or Declined
    // =========================================================

    /**
     * Sent immediately to Super Admins, Admins, and assigned Editor when a peer reviewer
     * accepts or declines a review assignment invitation.
     */
    public void sendAdminReviewerResponseNotification(
            Submission submission,
            ReviewAssignment assignment,
            User reviewer,
            boolean accepted,
            List<User> recipients) {
        if (recipients == null || recipients.isEmpty()) return;

        String reviewerName = (reviewer.getFullName() != null && !reviewer.getFullName().isBlank())
                ? reviewer.getFullName()
                : reviewer.getEmail();
        String reviewerEmail = reviewer.getEmail();

        String dueStr = assignment.getDueDate() != null
                ? DATE_FMT.format(assignment.getDueDate())
                : "Standard deadline (14 days)";

        String subject = accepted
                ? "GBJ Review Alert: Reviewer Accepted Invitation — " + submission.getSubmissionId()
                : "GBJ Review Alert: Reviewer Declined Invitation — " + submission.getSubmissionId();

        for (User recipient : recipients) {
            if (recipient.getEmail() == null || recipient.getEmail().isBlank()) continue;

            Map<String, Object> vars = new HashMap<>();
            vars.put("adminName", recipient.getFullName() != null && !recipient.getFullName().isBlank()
                    ? recipient.getFullName() : "Editorial Member");
            vars.put("submissionId", submission.getSubmissionId());
            vars.put("title", submission.getTitle());
            vars.put("articleType", submission.getType() != null ? submission.getType() : "Research Article");
            vars.put("reviewerName", reviewerName);
            vars.put("reviewerEmail", reviewerEmail);
            vars.put("accepted", accepted);
            vars.put("statusLabel", accepted ? "Accepted" : "Declined");
            vars.put("headerClass", accepted ? "header-accept" : "header-reject");
            vars.put("dueDate", dueStr);
            vars.put("respondedAt", DATE_FMT.format(Instant.now()));
            vars.put("pipelineUrl", mailProperties.getJournalUrl() + "/dashboard/pipeline");

            emailService.sendHtml(
                    recipient.getEmail(),
                    subject,
                    "email/admin-reviewer-response",
                    vars);

            log.info("Admin reviewer response email ({}) queued for recipient {} ({})",
                    accepted ? "ACCEPTED" : "DECLINED", recipient.getEmail(), submission.getSubmissionId());
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private record DecisionTexts(String label, String headerClass, String chipClass,
                                 String intro, String closing) {}

    private DecisionTexts buildDecisionTexts(Submission.SubmissionStatus status) {
        return switch (status) {
            case ACCEPTED -> new DecisionTexts(
                    "Accepted",
                    "header-accept",
                    "chip-accept",
                    "We are delighted to inform you that the editorial board has ACCEPTED your manuscript "
                    + "for publication in the Gono Bishwabidyalay Journal. Congratulations!",
                    "Your manuscript will now proceed to copyediting and production. "
                    + "We will be in touch shortly with further instructions. "
                    + "On behalf of the editorial board, we thank you for contributing your research to GBJ.");

            case REJECTED -> new DecisionTexts(
                    "Rejected",
                    "header-reject",
                    "chip-reject",
                    "After careful consideration and peer review, we regret to inform you that the editorial "
                    + "board has decided not to accept your manuscript for publication at this time.",
                    "We encourage you to review the editor's comments carefully, revise your manuscript, "
                    + "and consider resubmitting to another suitable venue. "
                    + "We appreciate your interest in publishing with GBJ and thank you for your submission.");

            case REVISION_REQUESTED -> new DecisionTexts(
                    "Revision Requested",
                    "header-revision",
                    "chip-revision",
                    "The editorial board has reviewed your manuscript and has determined that it requires "
                    + "revision before a final decision can be made. "
                    + "Please review the editor's notes below carefully.",
                    "Please upload your revised manuscript through the author portal. "
                    + "Ensure your revision addresses all reviewer and editor comments. "
                    + "We look forward to receiving your revised submission.");

            case SCHEDULED -> new DecisionTexts(
                    "Scheduled for Publication",
                    "header-accept",
                    "chip-accept",
                    "Excellent news! Your manuscript has been finalised and scheduled for publication "
                    + "in an upcoming issue of the Gono Bishwabidyalay Journal.",
                    "We will notify you when your article has been published and is publicly accessible. "
                    + "Thank you for your contribution to GBJ.");

            case PUBLISHED -> new DecisionTexts(
                    "Published",
                    "header-accept",
                    "chip-accept",
                    "Congratulations! Your manuscript has been officially published in the "
                    + "Gono Bishwabidyalay Journal and is now publicly accessible.",
                    "Please share your article with your colleagues, institution, and on your professional "
                    + "profiles. A DOI link is available in your author portal. Thank you for publishing with GBJ!");

            case COPYEDITING -> new DecisionTexts(
                    "In Copyediting",
                    "header-default",
                    "chip-default",
                    "Your accepted manuscript has entered the copyediting stage. "
                    + "Our editorial team will review the text for clarity, grammar, and formatting.",
                    "You may be contacted to approve minor changes. "
                    + "We will keep you updated as your article moves through production.");

            default -> new DecisionTexts(
                    status.name().replace('_', ' '),
                    "header-default",
                    "chip-default",
                    "There has been an update to the status of your manuscript submission.",
                    "Please log in to your author portal to view the latest details.");
        };
    }
}
