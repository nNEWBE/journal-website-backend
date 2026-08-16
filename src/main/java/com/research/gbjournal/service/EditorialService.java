package com.research.gbjournal.service;

import com.research.gbjournal.dto.editorial.EditorialDecisionRequest;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.entity.Issue;
import com.research.gbjournal.entity.Submission;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.ArticleRepository;
import com.research.gbjournal.repository.IssueRepository;
import com.research.gbjournal.repository.SubmissionRepository;
import com.research.gbjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorialService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final IssueRepository issueRepository;
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

    // ===== Get Single Submission =====

    @Transactional(readOnly = true)
    public SubmissionResponseDTO getSubmissionById(Long submissionId) {
        Submission submission = getSubmission(submissionId);
        return submissionService.toResponseDTO(submission);
    }

    // ===== Available Reviewers List =====

    @Transactional(readOnly = true)
    public List<com.research.gbjournal.dto.auth.AuthResponse.UserInfo> getAvailableReviewers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.REVIEWER
                        || u.getRole() == User.Role.EDITOR
                        || u.getRole() == User.Role.ADMIN
                        || u.getRole() == User.Role.SUPER_ADMIN)
                .map(u -> com.research.gbjournal.dto.auth.AuthResponse.UserInfo.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRole().name().toLowerCase().replace('_', '-'))
                        .title(u.getTitle())
                        .department(u.getDepartment())
                        .institution(u.getInstitution())
                        .avatarUrl(u.getAvatarUrl())
                        .emailVerified(u.isEmailVerified())
                        .build())
                .toList();
    }

    // ===== Publish Submission as Article =====

    @Transactional
    public SubmissionResponseDTO publishSubmission(Long submissionId, Long issueId, String doi, String pages) {
        Submission submission = getSubmission(submissionId);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        submission.setStatus(Submission.SubmissionStatus.PUBLISHED);
        submissionRepository.save(submission);

        String slug = generateSlug(submission.getTitle());
        long articleCount = articleRepository.count() + 1;
        String articleId = String.format("ART-2026-%03d", articleCount);

        com.research.gbjournal.entity.Article article = com.research.gbjournal.entity.Article.builder()
                .articleId(articleId)
                .slug(slug)
                .title(submission.getTitle())
                .type(submission.getType() != null ? submission.getType() : "Research Article")
                .topic(submission.getTopic() != null ? submission.getTopic() : "General Medicine")
                .department(submission.getSubmittingAuthor() != null ? submission.getSubmittingAuthor().getDepartment() : "General Research")
                .abstractText(submission.getAbstractText())
                .issue(issue)
                .issueLabel(issue.getIssueLabel())
                .volumeLabel(issue.getVolumeLabel())
                .pages(pages != null && !pages.isBlank() ? pages : "1-15")
                .doi(doi != null && !doi.isBlank() ? doi : "10.5555/gbj.2026." + String.format("%03d", articleCount))
                .publishedAt(issue.getMonth())
                .openAccess(true)
                .pdfAvailable(true)
                .build();

        // Authors
        List<com.research.gbjournal.entity.ArticleAuthor> authors = new java.util.ArrayList<>();
        if (submission.getAuthors() != null && !submission.getAuthors().isEmpty()) {
            for (var subAuthor : submission.getAuthors()) {
                authors.add(com.research.gbjournal.entity.ArticleAuthor.builder()
                        .article(article)
                        .name(subAuthor.getName())
                        .affiliation(subAuthor.getAffiliation())
                        .authorOrder(subAuthor.getAuthorOrder())
                        .corresponding(subAuthor.isCorresponding())
                        .build());
            }
        } else if (submission.getSubmittingAuthor() != null) {
            authors.add(com.research.gbjournal.entity.ArticleAuthor.builder()
                    .article(article)
                    .name(submission.getSubmittingAuthor().getFullName())
                    .affiliation(submission.getSubmittingAuthor().getDepartment())
                    .authorOrder(1)
                    .corresponding(true)
                    .build());
        }
        article.setAuthors(authors);

        // Keywords
        if (submission.getKeywords() != null && !submission.getKeywords().isBlank()) {
            List<com.research.gbjournal.entity.ArticleKeyword> keywords = new java.util.ArrayList<>();
            for (String kw : submission.getKeywords().split(",")) {
                if (!kw.trim().isEmpty()) {
                    keywords.add(com.research.gbjournal.entity.ArticleKeyword.builder()
                            .article(article)
                            .keyword(kw.trim())
                            .build());
                }
            }
            article.setKeywords(keywords);
        }

        // Sections
        List<com.research.gbjournal.entity.ArticleSection> sections = new java.util.ArrayList<>();
        sections.add(com.research.gbjournal.entity.ArticleSection.builder()
                .article(article)
                .heading("Abstract")
                .body(submission.getAbstractText())
                .sortOrder(1)
                .build());
        sections.add(com.research.gbjournal.entity.ArticleSection.builder()
                .article(article)
                .heading("Conclusion")
                .body("The findings provide significant insights and practical guidance in this academic field.")
                .sortOrder(2)
                .build());
        article.setSections(sections);

        articleRepository.save(article);
        issue.setArticleCount(issue.getArticleCount() + 1);
        issueRepository.save(issue);

        submissionMailService.sendStatusChangeNotification(submission);
        log.info("Submission {} published as article {} in issue {}", submission.getSubmissionId(), article.getArticleId(), issue.getIssueKey());

        return submissionService.toResponseDTO(submission);
    }

    // ===== Helpers =====

    private String generateSlug(String title) {
        String base = title.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        if (base.length() > 80) {
            base = base.substring(0, 80);
        }
        return base + "-" + System.currentTimeMillis() % 10000;
    }

    private Submission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", id));
    }
}
