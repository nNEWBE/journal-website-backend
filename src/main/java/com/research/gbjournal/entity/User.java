package com.research.gbjournal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 200)
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(max = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 30)
    private Role role;

    @Size(max = 200)
    private String institution;

    @Size(max = 150)
    private String department;

    @Size(max = 100)
    private String country;

    @Size(max = 50)
    private String orcid;

    @Column(columnDefinition = "TEXT")
    private String researchInterests;

    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    @Size(max = 100)
    private String title;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private boolean emailVerified = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /** Roles supported by the system */
    public enum Role {
        AUTHOR,
        REVIEWER,
        EDITOR,
        ADMIN,
        SUPER_ADMIN
    }
}
