package com.research.gbjournal.dto.content;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageContentDTO {

    private Long id;

    @NotBlank(message = "pageKey is required")
    private String pageKey;

    @NotBlank(message = "sectionKey is required")
    private String sectionKey;

    @NotBlank(message = "title is required")
    private String title;

    private String subtitle;
    private String content;
    private String metaJson;
    private int displayOrder;
    private boolean published;
    private String lastUpdatedBy;
    private LocalDateTime updatedAt;
}
