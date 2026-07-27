package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String issueKey;    // e.g. "2026-2"

    @Column(name = "issue_year", nullable = false, length = 10)
    private String year;

    @Column(nullable = false, length = 80)
    private String volumeLabel; // e.g. "Volume 4"

    @Column(nullable = false, length = 80)
    private String issueLabel;  // e.g. "Issue 2"

    @Column(name = "issue_month", nullable = false, length = 50)
    private String month;       // e.g. "July 2026"

    @Column(length = 300)
    private String theme;

    @Builder.Default
    private int articleCount = 0;

    @Column(name = "is_current")
    @Builder.Default
    private boolean current = false;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(columnDefinition = "TEXT")
    private String editorNote;

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Article> articles = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
