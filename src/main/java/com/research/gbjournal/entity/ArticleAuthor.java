package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "article_authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String affiliation;

    private int authorOrder;

    @Builder.Default
    private boolean corresponding = false;
}
