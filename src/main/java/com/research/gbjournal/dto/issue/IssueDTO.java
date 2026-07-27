package com.research.gbjournal.dto.issue;

import com.research.gbjournal.dto.article.ArticleDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class IssueDTO {

    private Long id;
    private String issueKey;
    private String year;
    private String volumeLabel;
    private String issueLabel;
    private String month;
    private String theme;
    private int articleCount;
    private boolean current;
    private String coverImageUrl;
    private String editorNote;
    private List<ArticleDTO> articles;

    /** Articles grouped by type for the current issue table of contents */
    private Map<String, List<ArticleDTO>> articlesByType;
}
