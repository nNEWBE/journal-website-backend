package com.research.gbjournal.controller;

import com.research.gbjournal.dto.admin.DashboardStatsDTO;
import com.research.gbjournal.dto.issue.IssueDTO;
import com.research.gbjournal.service.DashboardService;
import com.research.gbjournal.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final IssueService issueService;

    /** GET /api/v1/admin/stats */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    /** PUT /api/v1/admin/issues/{id}/set-current */
    @PutMapping("/issues/{id}/set-current")
    public ResponseEntity<IssueDTO> setCurrentIssue(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.setCurrentIssue(id));
    }
}
