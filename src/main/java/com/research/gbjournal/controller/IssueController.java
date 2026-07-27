package com.research.gbjournal.controller;

import com.research.gbjournal.dto.issue.IssueDTO;
import com.research.gbjournal.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    /** GET /api/v1/issues */
    @GetMapping
    public ResponseEntity<List<IssueDTO>> listIssues() {
        return ResponseEntity.ok(issueService.getAllIssues());
    }

    /** GET /api/v1/issues/current */
    @GetMapping("/current")
    public ResponseEntity<IssueDTO> getCurrentIssue() {
        return ResponseEntity.ok(issueService.getCurrentIssue());
    }

    /** GET /api/v1/issues/{issueKey} — e.g. /issues/2026-2 */
    @GetMapping("/{issueKey}")
    public ResponseEntity<IssueDTO> getIssue(@PathVariable String issueKey) {
        return ResponseEntity.ok(issueService.getIssueByKey(issueKey));
    }
}
