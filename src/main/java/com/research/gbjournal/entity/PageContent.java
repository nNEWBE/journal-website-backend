package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "page_contents", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"page_key", "section_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_key", nullable = false, length = 60)
    private String pageKey; // e.g. "about", "authors", "policies", "announcements", "contact", "home"

    @Column(name = "section_key", nullable = false, length = 80)
    private String sectionKey; // e.g. "overview", "indexing", "aims-scope", "guidelines", "ethics"

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content; // Markdown / HTML formatted text

    @Lob
    @Column(name = "meta_json", columnDefinition = "TEXT")
    private String metaJson; // Structured JSON for checklists, badges, stats, accordions

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = true;

    @Column(name = "last_updated_by", length = 120)
    private String lastUpdatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
