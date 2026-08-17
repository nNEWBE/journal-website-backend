package com.research.gbjournal.controller;

import com.research.gbjournal.dto.content.PageContentDTO;
import com.research.gbjournal.service.PageContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class PageContentController {

    private final PageContentService pageContentService;

    /** GET /api/v1/content/{pageKey} — Public published content for a given page */
    @GetMapping("/{pageKey}")
    public ResponseEntity<List<PageContentDTO>> getPageContent(@PathVariable String pageKey) {
        return ResponseEntity.ok(pageContentService.getPublishedPageContent(pageKey));
    }

    /** GET /api/v1/content/all — Public all published page sections */
    @GetMapping("/all")
    public ResponseEntity<Map<String, List<PageContentDTO>>> getAllContent() {
        return ResponseEntity.ok(pageContentService.getAllPagesContent());
    }
}
