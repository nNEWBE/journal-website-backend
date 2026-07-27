package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "review_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.INVITED;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ReviewRecommendation recommendation;

    @Column(columnDefinition = "TEXT")
    private String reviewComments;

    @Column(columnDefinition = "TEXT")
    private String confidentialComments;

    /** 1-100 quality score */
    private Integer score;

    private Instant dueDate;

    private Instant reviewSubmittedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant invitedAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum ReviewStatus {
        INVITED,
        ACCEPTED,
        DECLINED,
        COMPLETED
    }

    public enum ReviewRecommendation {
        ACCEPT,
        MINOR_REVISION,
        MAJOR_REVISION,
        REJECT,
        ACCEPT_WITH_REVISIONS
    }
}
