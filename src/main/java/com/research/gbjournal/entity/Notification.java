package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "site_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(length = 50)
    @Builder.Default
    private String type = "system"; // "review", "submission", "editorial", "system"

    /** Comma-separated target roles, e.g. "editor,admin,super_admin" */
    @Column(length = 150)
    private String targetRoles;

    @Column(length = 255)
    private String link;

    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
