package com.research.gbjournal.controller;

import com.research.gbjournal.dto.article.ArticleDTO;
import com.research.gbjournal.dto.article.ArticleDetailDTO;
import com.research.gbjournal.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * GET /api/v1/articles
     * Supports: query, type, topic, issue, page, size, sort
     */
    @GetMapping
    public ResponseEntity<Page<ArticleDTO>> listArticles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String issue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        return ResponseEntity.ok(
                articleService.searchArticles(query, type, topic, issue, page, size, sort));
    }

    /**
     * GET /api/v1/articles/{slug}
     * Returns full article detail and increments view count.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ArticleDetailDTO> getArticle(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.getArticleBySlug(slug));
    }

    /**
     * GET /api/v1/article-types
     * Returns distinct article types from the database.
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getArticleTypes() {
        return ResponseEntity.ok(articleService.getArticleTypes());
    }

    /**
     * GET /api/v1/articles/topics
     * Returns distinct topics.
     */
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getTopics() {
        return ResponseEntity.ok(articleService.getTopics());
    }

    /**
     * POST /api/v1/articles/{slug}/download
     * Tracks a PDF download and returns the article's pdfUrl for redirect.
     */
    @PostMapping("/{slug}/download")
    public ResponseEntity<String> trackDownload(@PathVariable String slug) {
        String pdfUrl = articleService.trackDownload(slug).getPdfUrl();
        return ResponseEntity.ok(pdfUrl != null ? pdfUrl : "");
    }
}
