package com.research.gbjournal.service;

import com.research.gbjournal.dto.article.ArticleDTO;
import com.research.gbjournal.dto.article.ArticleDetailDTO;
import com.research.gbjournal.entity.Article;
import com.research.gbjournal.entity.ArticleAuthor;
import com.research.gbjournal.entity.ArticleKeyword;
import com.research.gbjournal.entity.ArticleSection;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    // ===== Search / List =====

    @Transactional(readOnly = true)
    public Page<ArticleDTO> searchArticles(String query, String type, String topic,
                                            String issueLabel, int page, int size, String sort) {
        String nullableQuery = (query == null || query.isBlank()) ? null : query.trim();
        String nullableType = (type == null || type.isBlank()) ? null : type.trim();
        String nullableTopic = (topic == null || topic.isBlank()) ? null : topic.trim();
        String nullableIssue = (issueLabel == null || issueLabel.isBlank()) ? null : issueLabel.trim();

        Sort sortOrder = buildSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);

        return articleRepository.searchArticles(nullableQuery, nullableType, nullableTopic, nullableIssue, pageable)
                .map(this::toDTO);
    }

    // ===== Article Detail =====

    @Transactional
    public ArticleDetailDTO getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));

        // Increment view count
        articleRepository.incrementViews(article.getId());

        return toDetailDTO(article);
    }

    // ===== PDF Download tracking =====

    @Transactional
    public Article trackDownload(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));
        articleRepository.incrementDownloads(article.getId());
        return article;
    }

    // ===== Metadata =====

    @Transactional(readOnly = true)
    public List<String> getArticleTypes() {
        return articleRepository.findAllArticleTypes();
    }

    @Transactional(readOnly = true)
    public List<String> getTopics() {
        return articleRepository.findAllTopics();
    }

    // ===== Mappers =====

    public ArticleDTO toDTO(Article a) {
        return ArticleDTO.builder()
                .id(a.getId())
                .articleId(a.getArticleId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .type(a.getType())
                .topic(a.getTopic())
                .department(a.getDepartment())
                .authors(a.getAuthors().stream().map(ArticleAuthor::getName).toList())
                .abstractText(a.getAbstractText())
                .issueLabel(a.getIssueLabel())
                .volumeLabel(a.getVolumeLabel())
                .pages(a.getPages())
                .doi(a.getDoi())
                .publishedAt(a.getPublishedAt())
                .metrics(ArticleDTO.MetricsDTO.builder()
                        .views(a.getMetrics().getViews())
                        .downloads(a.getMetrics().getDownloads())
                        .citations(a.getMetrics().getCitations())
                        .build())
                .keywords(a.getKeywords().stream().map(ArticleKeyword::getKeyword).toList())
                .imageUrl(a.getImageUrl())
                .openAccess(a.isOpenAccess())
                .pdfAvailable(a.isPdfAvailable())
                .build();
    }

    public ArticleDetailDTO toDetailDTO(Article a) {
        return ArticleDetailDTO.builder()
                .id(a.getId())
                .articleId(a.getArticleId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .type(a.getType())
                .topic(a.getTopic())
                .department(a.getDepartment())
                .authors(a.getAuthors().stream().map(author ->
                        ArticleDetailDTO.AuthorInfo.builder()
                                .name(author.getName())
                                .affiliation(author.getAffiliation())
                                .authorOrder(author.getAuthorOrder())
                                .corresponding(author.isCorresponding())
                                .build()).toList())
                .abstractText(a.getAbstractText())
                .issueLabel(a.getIssueLabel())
                .volumeLabel(a.getVolumeLabel())
                .pages(a.getPages())
                .doi(a.getDoi())
                .publishedAt(a.getPublishedAt())
                .metrics(ArticleDTO.MetricsDTO.builder()
                        .views(a.getMetrics().getViews())
                        .downloads(a.getMetrics().getDownloads())
                        .citations(a.getMetrics().getCitations())
                        .build())
                .keywords(a.getKeywords().stream().map(ArticleKeyword::getKeyword).toList())
                .sections(a.getSections().stream().map(s ->
                        ArticleDetailDTO.SectionDTO.builder()
                                .heading(s.getHeading())
                                .body(s.getBody())
                                .sortOrder(s.getSortOrder())
                                .build()).toList())
                .imageUrl(a.getImageUrl())
                .pdfUrl(a.getPdfUrl())
                .openAccess(a.isOpenAccess())
                .pdfAvailable(a.isPdfAvailable())
                .build();
    }

    private Sort buildSort(String sort) {
        return switch (sort == null ? "newest" : sort.toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "views" -> Sort.by(Sort.Direction.DESC, "metrics.views");
            case "downloads" -> Sort.by(Sort.Direction.DESC, "metrics.downloads");
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // newest
        };
    }
}
