package com.research.gbjournal.dto.article;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ArticleDetailDTO {

    private Long id;
    private String articleId;
    private String slug;
    private String title;
    private String type;
    private String topic;
    private String department;
    private List<AuthorInfo> authors;
    private String abstractText;
    private String issueLabel;
    private String volumeLabel;
    private String pages;
    private String doi;
    private String publishedAt;
    private ArticleDTO.MetricsDTO metrics;
    private List<String> keywords;
    private List<SectionDTO> sections;
    private String imageUrl;
    private String pdfUrl;
    private boolean openAccess;
    private boolean pdfAvailable;

    @Data
    @Builder
    public static class AuthorInfo {
        private String name;
        private String affiliation;
        private int authorOrder;
        private boolean corresponding;
    }

    @Data
    @Builder
    public static class SectionDTO {
        private String heading;
        private String body;
        private int sortOrder;
    }
}
