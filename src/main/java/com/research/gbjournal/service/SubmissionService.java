package com.research.gbjournal.service;

import com.research.gbjournal.dto.submission.SubmissionCreateRequest;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.entity.*;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.SubmissionFileRepository;
import com.research.gbjournal.repository.SubmissionRepository;
import com.research.gbjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionFileRepository submissionFileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final SubmissionMailService submissionMailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ===== Create / Save Draft =====

    @Transactional
    public SubmissionResponseDTO createOrUpdateDraft(String authorEmail, SubmissionCreateRequest request) {
        User author = userRepository.findByEmailIgnoreCase(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        Submission submission = Submission.builder()
                .submissionId(generateSubmissionId())
                .title(request.getTitle())
                .runningTitle(request.getRunningTitle())
                .type(request.getType())
                .abstractText(request.getAbstractText())
                .keywords(request.getKeywords())
                .topic(request.getTopic())
                .coverLetter(request.getCoverLetter())
                .status(Submission.SubmissionStatus.DRAFT)
                .submittingAuthor(author)
                .conflictOfInterest(request.getConflictOfInterest())
                .fundingStatement(request.getFundingStatement())
                .ethicsStatement(request.getEthicsStatement())
                .dataAvailability(request.getDataAvailability())
                .aiDeclaration(request.getAiDeclaration())
                .copyrightAgreed(request.isCopyrightAgreed())
                .build();

        // Add co-authors
        if (request.getAuthors() != null) {
            List<SubmissionAuthor> authors = new ArrayList<>();
            for (var coAuthorReq : request.getAuthors()) {
                authors.add(SubmissionAuthor.builder()
                        .submission(submission)
                        .name(coAuthorReq.getName())
                        .email(coAuthorReq.getEmail())
                        .affiliation(coAuthorReq.getAffiliation())
                        .orcid(coAuthorReq.getOrcid())
                        .authorOrder(coAuthorReq.getAuthorOrder())
                        .corresponding(coAuthorReq.isCorresponding())
                        .build());
            }
            submission.setAuthors(authors);
        }

        submissionRepository.save(submission);
        log.info("Draft created: {} by {}", submission.getSubmissionId(), authorEmail);
        return toResponseDTO(submission);
    }

    // ===== Update Existing Draft =====

    @Transactional
    public SubmissionResponseDTO updateDraft(String authorEmail, Long submissionId, SubmissionCreateRequest request) {
        Submission submission = getOwnedSubmission(authorEmail, submissionId);

        if (submission.getStatus() != Submission.SubmissionStatus.DRAFT &&
            submission.getStatus() != Submission.SubmissionStatus.REVISION_REQUESTED) {
            throw new BadRequestException("Only DRAFT or REVISION_REQUESTED manuscripts can be edited directly.");
        }

        submission.setTitle(request.getTitle());
        submission.setRunningTitle(request.getRunningTitle());
        submission.setType(request.getType());
        submission.setAbstractText(request.getAbstractText());
        submission.setKeywords(request.getKeywords());
        submission.setTopic(request.getTopic());
        submission.setCoverLetter(request.getCoverLetter());
        submission.setConflictOfInterest(request.getConflictOfInterest());
        submission.setFundingStatement(request.getFundingStatement());
        submission.setEthicsStatement(request.getEthicsStatement());
        submission.setDataAvailability(request.getDataAvailability());
        submission.setAiDeclaration(request.getAiDeclaration());
        submission.setCopyrightAgreed(request.isCopyrightAgreed());

        if (request.getAuthors() != null) {
            submission.getAuthors().clear();
            for (var coAuthorReq : request.getAuthors()) {
                submission.getAuthors().add(SubmissionAuthor.builder()
                        .submission(submission)
                        .name(coAuthorReq.getName())
                        .email(coAuthorReq.getEmail())
                        .affiliation(coAuthorReq.getAffiliation())
                        .orcid(coAuthorReq.getOrcid())
                        .authorOrder(coAuthorReq.getAuthorOrder())
                        .corresponding(coAuthorReq.isCorresponding())
                        .build());
            }
        }

        submissionRepository.save(submission);
        log.info("Draft updated: {} by {}", submission.getSubmissionId(), authorEmail);
        return toResponseDTO(submission);
    }

    // ===== Submit Revision =====

    @Transactional
    public SubmissionResponseDTO submitRevision(String authorEmail, Long submissionId, String revisionNotes) {
        Submission submission = getOwnedSubmission(authorEmail, submissionId);

        if (submission.getStatus() != Submission.SubmissionStatus.REVISION_REQUESTED &&
            submission.getStatus() != Submission.SubmissionStatus.DRAFT) {
            throw new BadRequestException("Submission is not in REVISION_REQUESTED status.");
        }

        submission.setStatus(Submission.SubmissionStatus.UNDER_REVIEW);
        if (revisionNotes != null && !revisionNotes.isBlank()) {
            submission.setCoverLetter((submission.getCoverLetter() != null ? submission.getCoverLetter() + "\n\n[Revision Notes]: " : "[Revision Notes]: ") + revisionNotes);
        }
        submissionRepository.save(submission);

        log.info("Revision submitted: {} by {}", submission.getSubmissionId(), authorEmail);
        return toResponseDTO(submission);
    }

    // ===== Submit (move from DRAFT -> SUBMITTED) =====

    @Transactional
    public SubmissionResponseDTO submitManuscript(String authorEmail, Long submissionId) {
        Submission submission = getOwnedSubmission(authorEmail, submissionId);

        if (submission.getStatus() != Submission.SubmissionStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT submissions can be submitted.");
        }

        submission.setStatus(Submission.SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(Instant.now());
        submissionRepository.save(submission);

        // Async email — does not block the HTTP response
        submissionMailService.sendSubmissionConfirmation(submission);

        log.info("Submission {} submitted by {}", submission.getSubmissionId(), authorEmail);
        return toResponseDTO(submission);
    }

    // ===== Upload File =====

    @Transactional
    public SubmissionResponseDTO uploadFile(String authorEmail, Long submissionId,
            MultipartFile file, String fileType) {
        Submission submission = getOwnedSubmission(authorEmail, submissionId);

        SubmissionFile.FileType type;
        try {
            type = SubmissionFile.FileType.valueOf(fileType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid file type: " + fileType);
        }

        String storedFilename = fileStorageService.store(file, "submissions/" + submission.getSubmissionId());

        SubmissionFile submissionFile = SubmissionFile.builder()
                .submission(submission)
                .fileType(type)
                .storedFilename(storedFilename)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();

        submission.getFiles().add(submissionFile);
        submissionRepository.save(submission);

        return toResponseDTO(submission);
    }

    // ===== Withdraw =====

    @Transactional
    public SubmissionResponseDTO withdrawSubmission(String authorEmail, Long submissionId) {
        Submission submission = getOwnedSubmission(authorEmail, submissionId);

        if (submission.getStatus() == Submission.SubmissionStatus.PUBLISHED ||
                submission.getStatus() == Submission.SubmissionStatus.WITHDRAWN) {
            throw new BadRequestException("This submission cannot be withdrawn.");
        }

        submission.setStatus(Submission.SubmissionStatus.WITHDRAWN);
        submissionRepository.save(submission);

        // Async email — withdrawal confirmation
        submissionMailService.sendWithdrawalConfirmation(submission);

        return toResponseDTO(submission);
    }

    // ===== Author: Get My Submissions =====

    @Transactional(readOnly = true)
    public List<SubmissionResponseDTO> getMySubmissions(String authorEmail) {
        User author = userRepository.findByEmailIgnoreCase(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));
        return submissionRepository.findBySubmittingAuthorOrderByUpdatedAtDesc(author)
                .stream().map(sub -> toResponseDTO(sub)).toList();
    }

    // ===== Get Single Submission =====

    @Transactional(readOnly = true)
    public SubmissionResponseDTO getSubmission(String userEmail, Long submissionId) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));

        // Authors can only see their own; editors/admins can see all
        boolean isEditor = user.getRole() == User.Role.EDITOR
                || user.getRole() == User.Role.ADMIN
                || user.getRole() == User.Role.SUPER_ADMIN;
        boolean isOwner = submission.getSubmittingAuthor().getId().equals(user.getId());

        if (!isEditor && !isOwner) {
            throw new BadRequestException("You are not authorized to view this submission.");
        }

        return toResponseDTO(submission);
    }

    // ===== All submissions (Editor/Admin) =====

    @Transactional(readOnly = true)
    public Page<SubmissionResponseDTO> getAllSubmissions(String status, String type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Submission.SubmissionStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Submission.SubmissionStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        String nullableType = (type == null || type.isBlank()) ? null : type.trim();
        return submissionRepository.findByStatusAndType(statusEnum, nullableType, pageable)
                .map(sub -> toResponseDTO(sub));
    }

    // ===== Helpers =====

    private Submission getOwnedSubmission(String authorEmail, Long submissionId) {
        User author = userRepository.findByEmailIgnoreCase(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));
        if (!submission.getSubmittingAuthor().getId().equals(author.getId())) {
            throw new BadRequestException("You are not the owner of this submission.");
        }
        return submission;
    }

    private String generateSubmissionId() {
        int year = java.time.LocalDate.now().getYear();
        long count = submissionRepository.count() + 100;
        return "GBJ-%d-%03d".formatted(year, count);
    }

    // ===== DTO Mapper =====

    public SubmissionResponseDTO toResponseDTO(Submission s) {
        return SubmissionResponseDTO.builder()
                .id(s.getId())
                .submissionId(s.getSubmissionId())
                .title(s.getTitle())
                .runningTitle(s.getRunningTitle())
                .type(s.getType())
                .abstractText(s.getAbstractText())
                .keywords(s.getKeywords())
                .topic(s.getTopic())
                .coverLetter(s.getCoverLetter())
                .status(s.getStatus().name())
                .submittingAuthor(SubmissionResponseDTO.AuthorInfo.builder()
                        .id(s.getSubmittingAuthor().getId())
                        .fullName(s.getSubmittingAuthor().getFullName())
                        .email(s.getSubmittingAuthor().getEmail())
                        .department(s.getSubmittingAuthor().getDepartment())
                        .institution(s.getSubmittingAuthor().getInstitution())
                        .build())
                .assignedEditor(s.getAssignedEditor() == null ? null
                        : SubmissionResponseDTO.EditorInfo.builder()
                                .id(s.getAssignedEditor().getId())
                                .fullName(s.getAssignedEditor().getFullName())
                                .email(s.getAssignedEditor().getEmail())
                                .build())
                .authors(s.getAuthors().stream().map(a -> SubmissionResponseDTO.CoAuthorDTO.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .email(a.getEmail())
                        .affiliation(a.getAffiliation())
                        .orcid(a.getOrcid())
                        .authorOrder(a.getAuthorOrder())
                        .corresponding(a.isCorresponding())
                        .build()).toList())
                .files(s.getFiles().stream().map(f -> SubmissionResponseDTO.FileDTO.builder()
                        .id(f.getId())
                        .fileType(f.getFileType().name())
                        .originalFilename(f.getOriginalFilename())
                        .contentType(f.getContentType())
                        .sizeBytes(f.getSizeBytes())
                        .downloadUrl(baseUrl + "/api/v1/files/" + f.getStoredFilename())
                        .uploadedAt(f.getUploadedAt())
                        .build()).toList())
                .reviews(s.getReviews().stream().map(r -> SubmissionResponseDTO.ReviewDTO.builder()
                        .id(r.getId())
                        .reviewerName(r.getReviewer().getFullName())
                        .reviewerEmail(r.getReviewer().getEmail())
                        .status(r.getStatus().name())
                        .recommendation(r.getRecommendation() != null ? r.getRecommendation().name() : null)
                        .score(r.getScore())
                        .dueDate(r.getDueDate())
                        .reviewSubmittedAt(r.getReviewSubmittedAt())
                        .build()).toList())
                .conflictOfInterest(s.getConflictOfInterest())
                .fundingStatement(s.getFundingStatement())
                .ethicsStatement(s.getEthicsStatement())
                .dataAvailability(s.getDataAvailability())
                .aiDeclaration(s.getAiDeclaration())
                .copyrightAgreed(s.isCopyrightAgreed())
                .editorDecisionNote(s.getEditorDecisionNote())
                .decisionDate(s.getDecisionDate())
                .reviewScore(s.getReviewScore())
                .submittedAt(s.getSubmittedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
