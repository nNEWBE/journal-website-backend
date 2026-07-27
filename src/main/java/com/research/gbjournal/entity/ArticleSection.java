package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "article_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 200)
    private String heading;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private int sortOrder;
}
