package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable ID e.g. GBJ-2026-104 */
    @Column(nullable = false, unique = true, length = 30)
    private String submissionId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 100)
    private String runningTitle;

    @Column(name = "submission_type", nullable = false, length = 80)
    private String type;        // article type

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(columnDefinition = "TEXT")
    private String keywords;    // comma-separated

    @Column(length = 100)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitting_author_id", nullable = false)
    private User submittingAuthor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_editor_id")
    private User assignedEditor;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubmissionAuthor> authors = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubmissionFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewAssignment> reviews = new ArrayList<>();

    // Declarations
    @Column(columnDefinition = "TEXT")
    private String conflictOfInterest;

    @Column(columnDefinition = "TEXT")
    private String fundingStatement;

    @Column(columnDefinition = "TEXT")
    private String ethicsStatement;

    @Column(columnDefinition = "TEXT")
    private String dataAvailability;

    @Column(columnDefinition = "TEXT")
    private String aiDeclaration;

    @Builder.Default
    private boolean copyrightAgreed = false;

    // Editorial
    @Column(columnDefinition = "TEXT")
    private String editorDecisionNote;

    private Instant decisionDate;

    private int reviewScore;

    private Instant submittedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ===== Submission Status Lifecycle =====
    public enum SubmissionStatus {
        DRAFT,
        SUBMITTED,
        INITIAL_CHECK,
        WITH_EDITOR,
        REVIEWER_INVITATION,
        UNDER_REVIEW,
        REVIEWS_COMPLETE,
        REVISION_REQUESTED,
        REVISION_SUBMITTED,
        ACCEPTED,
        COPYEDITING,
        PROOFING,
        SCHEDULED,
        PUBLISHED,
        REJECTED,
        WITHDRAWN
    }
}
