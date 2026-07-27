package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "submission_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileType fileType;

    /** UUID-based filename stored on disk */
    @Column(nullable = false, length = 300)
    private String storedFilename;

    /** Original upload filename shown to users */
    @Column(nullable = false, length = 300)
    private String originalFilename;

    @Column(length = 100)
    private String contentType;

    private long sizeBytes;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant uploadedAt;

    public enum FileType {
        MANUSCRIPT,
        TITLE_PAGE,
        COVER_LETTER,
        FIGURE,
        TABLE,
        SUPPLEMENTARY,
        ETHICS_APPROVAL,
        CHECKLIST
    }
}
