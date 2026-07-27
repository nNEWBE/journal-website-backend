package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String articleId;   // e.g. ART-2026-001

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "article_type", nullable = false, length = 80)
    private String type;        // Research Article, Review Article, …

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(length = 150)
    private String department;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String abstractText;

    @Column(length = 80)
    private String issueLabel;  // e.g. "Issue 2"

    @Column(length = 80)
    private String volumeLabel; // e.g. "Volume 4"

    @Column(length = 30)
    private String pages;       // e.g. "11-28"

    @Column(length = 80)
    private String doi;

    @Column(length = 50)
    private String publishedAt; // e.g. "July 2026"

    /** Resolved FK to the Issue */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    /** View/download/citation counts */
    @Embedded
    @Builder.Default
    private ArticleMetrics metrics = new ArticleMetrics();

    /** Ordered article sections */
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ArticleSection> sections = new ArrayList<>();

    /** Authors listed on the published article */
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("authorOrder ASC")
    @Builder.Default
    private List<ArticleAuthor> authors = new ArrayList<>();

    /** Keywords */
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ArticleKeyword> keywords = new ArrayList<>();

    /** Publicly accessible PDF path / URL */
    @Column(length = 500)
    private String pdfUrl;

    /** Cover image path / URL */
    @Column(length = 500)
    private String imageUrl;

    @Builder.Default
    private boolean openAccess = true;

    @Builder.Default
    private boolean pdfAvailable = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
