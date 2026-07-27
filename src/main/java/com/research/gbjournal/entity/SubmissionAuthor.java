package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "submission_authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 200)
    private String email;

    @Column(length = 200)
    private String affiliation;

    @Column(length = 30)
    private String orcid;

    private int authorOrder;

    @Builder.Default
    private boolean corresponding = false;
}
