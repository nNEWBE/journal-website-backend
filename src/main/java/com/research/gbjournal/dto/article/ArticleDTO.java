package com.research.gbjournal.dto.article;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ArticleDTO {

    private Long id;
    private String articleId;
    private String slug;
    private String title;
    private String type;
    private String topic;
    private String department;
    private List<String> authors;
    private String abstractText;
    private String issueLabel;
    private String volumeLabel;
    private String pages;
    private String doi;
    private String publishedAt;
    private MetricsDTO metrics;
    private List<String> keywords;
    private String imageUrl;
    private boolean openAccess;
    private boolean pdfAvailable;

    @Data
    @Builder
    public static class MetricsDTO {
        private int views;
        private int downloads;
        private int citations;
    }

    public String getAbstract() {
        return abstractText;
    }

    public String getIssue() {
        return issueLabel;
    }

    public String getVolume() {
        return volumeLabel;
    }

    public String getImage() {
        return imageUrl;
    }

    public String getPdf() {
        return imageUrl != null ? imageUrl : "";
    }
}
