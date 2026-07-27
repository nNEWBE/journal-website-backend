package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String role;        // Editor-in-Chief, Managing Editor, Section Editor…

    @Column(length = 200)
    private String unit;        // Department or faculty

    @Column(length = 150)
    private String institution;

    @Column(length = 500)
    private String expertise;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 30)
    private String orcid;

    @Column(length = 200)
    private String googleScholarUrl;

    /** Controls display order on the editorial board page */
    private int sortOrder;
}
