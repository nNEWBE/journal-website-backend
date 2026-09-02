package com.research.gbjournal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "nav_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", length = 80)
    private String clientId;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 255)
    private String href;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "dropdown_header", length = 255)
    private String dropdownHeader;

    @Column(name = "footer_label", length = 120)
    private String footerLabel;

    @Column(name = "footer_href", length = 255)
    private String footerHref;

    @Column(name = "icon_name", length = 80)
    private String iconName;

    @Column(length = 300)
    private String description;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "open_in_new_tab", nullable = false)
    @Builder.Default
    private boolean openInNewTab = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
