package com.research.gbjournal.dto.submission;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class SubmissionResponseDTO {

    private Long id;
    private String submissionId;
    private String title;
    private String runningTitle;
    private String type;
    private String abstractText;
    private String keywords;
    private String topic;
    private String coverLetter;
    private String status;
    private AuthorInfo submittingAuthor;
    private EditorInfo assignedEditor;
    private List<CoAuthorDTO> authors;
    private List<FileDTO> files;
    private List<ReviewDTO> reviews;
    private String conflictOfInterest;
    private String fundingStatement;
    private String ethicsStatement;
    private String dataAvailability;
    private String aiDeclaration;
    private boolean copyrightAgreed;
    private String editorDecisionNote;
    private Instant decisionDate;
    private int reviewScore;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    public static class AuthorInfo {
        private Long id;
        private String fullName;
        private String email;
        private String department;
        private String institution;
    }

    @Data
    @Builder
    public static class EditorInfo {
        private Long id;
        private String fullName;
        private String email;
    }

    @Data
    @Builder
    public static class CoAuthorDTO {
        private Long id;
        private String name;
        private String email;
        private String affiliation;
        private String orcid;
        private int authorOrder;
        private boolean corresponding;
    }

    @Data
    @Builder
    public static class FileDTO {
        private Long id;
        private String fileType;
        private String originalFilename;
        private String contentType;
        private long sizeBytes;
        private String downloadUrl;
        private Instant uploadedAt;
    }

    @Data
    @Builder
    public static class ReviewDTO {
        private Long id;
        private String reviewerName;
        private String status;
        private String recommendation;
        private Integer score;
        private Instant dueDate;
        private Instant reviewSubmittedAt;
    }
}
