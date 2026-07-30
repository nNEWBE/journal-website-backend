package com.research.gbjournal.controller;

import com.research.gbjournal.dto.board.BoardMemberDTO;
import com.research.gbjournal.service.ArticleService;
import com.research.gbjournal.service.BoardMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MetadataController {

    private final ArticleService articleService;
    private final BoardMemberService boardMemberService;

    /** GET /api/v1/topics — Public list of all topics */
    @GetMapping("/api/v1/topics")
    public ResponseEntity<List<String>> getTopics() {
        return ResponseEntity.ok(articleService.getTopics());
    }

    /** GET /api/v1/article-types — Public list of all article types */
    @GetMapping("/api/v1/article-types")
    public ResponseEntity<List<String>> getArticleTypes() {
        return ResponseEntity.ok(articleService.getArticleTypes());
    }

    /** GET /api/v1/editorial-board — Public list of all editorial board members */
    @GetMapping("/api/v1/editorial-board")
    public ResponseEntity<List<BoardMemberDTO>> getEditorialBoard() {
        return ResponseEntity.ok(boardMemberService.getAllMembers());
    }
}
