package com.research.gbjournal.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleMetrics {

    @Builder.Default
    private int views = 0;

    @Builder.Default
    private int downloads = 0;

    @Builder.Default
    private int citations = 0;
}
