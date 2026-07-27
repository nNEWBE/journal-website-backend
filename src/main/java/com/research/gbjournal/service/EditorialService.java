package com.research.gbjournal.service;

import com.research.gbjournal.dto.editorial.EditorialDecisionRequest;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.entity.Submission;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.SubmissionRepository;
import com.research.gbjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorialService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final SubmissionService submissionService;
    private final SubmissionMailService submissionMailService;

    // ===== Get all submissions for editor dashboard =====

    @Transactional(readOnly = true)
    public Page<SubmissionResponseDTO> listSubmissions(String status, String type, int page, int size) {
        return submissionService.getAllSubmissions(status, type, page, size);
    }

    // ===== Assign editor to submission =====

    @Transactional
    public SubmissionResponseDTO assignEditor(Long submissionId, Long editorId) {
        Submission submission = getSubmission(submissionId);
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", editorId));

        if (editor.getRole() != User.Role.EDITOR
                && editor.getRole() != User.Role.ADMIN
                && editor.getRole() != User.Role.SUPER_ADMIN) {
            throw new BadRequestException("Selected user is not an editor.");
        }

        submission.setAssignedEditor(editor);
        if (submission.getStatus() == Submission.SubmissionStatus.SUBMITTED
                || submission.getStatus() == Submission.SubmissionStatus.INITIAL_CHECK) {
            submission.setStatus(Submission.SubmissionStatus.WITH_EDITOR);
        }
        submissionRepository.save(submission);
        log.info("Editor {} assigned to submission {}", editor.getEmail(), submission.getSubmissionId());
        return submissionService.toResponseDTO(submission);
    }

    // ===== Make editorial decision =====

    @Transactional
    public SubmissionResponseDTO makeDecision(Long submissionId, EditorialDecisionRequest request) {
        Submission submission = getSubmission(submissionId);

        Submission.SubmissionStatus newStatus = switch (request.getDecision().toUpperCase()) {
            case "ACCEPT" -> Submission.SubmissionStatus.ACCEPTED;
            case "REJECT" -> Submission.SubmissionStatus.REJECTED;
            case "REVISION_REQUESTED" -> Submission.SubmissionStatus.REVISION_REQUESTED;
            default -> throw new BadRequestException("Invalid decision: " + request.getDecision());
        };

        submission.setStatus(newStatus);
        submission.setEditorDecisionNote(request.getNote());
        submission.setDecisionDate(Instant.now());
        submissionRepository.save(submission);

        // Async email notification to author — fires on background thread
        submissionMailService.sendStatusChangeNotification(submission);

        log.info("Editorial decision '{}' for submission {}", request.getDecision(), submission.getSubmissionId());
        return submissionService.toResponseDTO(submission);
    }

    // ===== Move to copyediting =====

    @Transactional
    public SubmissionResponseDTO moveToCopyediting(Long submissionId) {
        Submission submission = getSubmission(submissionId);
        if (submission.getStatus() != Submission.SubmissionStatus.ACCEPTED) {
            throw new BadRequestException("Only ACCEPTED submissions can move to copyediting.");
        }
        submission.setStatus(Submission.SubmissionStatus.COPYEDITING);
        submissionRepository.save(submission);

        // Notify author of production stage change
        submissionMailService.sendStatusChangeNotification(submission);

        return submissionService.toResponseDTO(submission);
    }

    // ===== Move to scheduled =====

    @Transactional
    public SubmissionResponseDTO scheduleForIssue(Long submissionId) {
        Submission submission = getSubmission(submissionId);
        if (submission.getStatus() != Submission.SubmissionStatus.PROOFING
                && submission.getStatus() != Submission.SubmissionStatus.COPYEDITING) {
            throw new BadRequestException("Submission must be in COPYEDITING or PROOFING to schedule.");
        }
        submission.setStatus(Submission.SubmissionStatus.SCHEDULED);
        submissionRepository.save(submission);

        // Notify author that their article is scheduled
        submissionMailService.sendStatusChangeNotification(submission);

        return submissionService.toResponseDTO(submission);
    }

    // ===== Helper =====

    private Submission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", id));
    }
}
