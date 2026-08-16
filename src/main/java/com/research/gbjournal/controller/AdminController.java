package com.research.gbjournal.controller;

import com.research.gbjournal.dto.admin.DashboardStatsDTO;
import com.research.gbjournal.dto.auth.AuthResponse;
import com.research.gbjournal.dto.board.BoardMemberDTO;
import com.research.gbjournal.dto.issue.IssueDTO;
import com.research.gbjournal.service.BoardMemberService;
import com.research.gbjournal.service.DashboardService;
import com.research.gbjournal.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final IssueService issueService;
    private final BoardMemberService boardMemberService;

    /** GET /api/v1/admin/stats */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    /** GET /api/v1/admin/users — List all registered users */
    @GetMapping("/users")
    public ResponseEntity<List<AuthResponse.UserInfo>> listUsers() {
        return ResponseEntity.ok(dashboardService.getAllUsers());
    }

    /** PUT /api/v1/admin/users/{id}/role — Update user role */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<AuthResponse.UserInfo> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        return ResponseEntity.ok(dashboardService.updateUserRole(id, role));
    }

    /** PUT /api/v1/admin/users/{id}/status — Enable or disable user */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<AuthResponse.UserInfo> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(dashboardService.updateUserStatus(id, enabled));
    }

    /** POST /api/v1/admin/issues — Create a new issue */
    @PostMapping("/issues")
    public ResponseEntity<IssueDTO> createIssue(@RequestBody IssueDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.createIssue(dto));
    }

    /** PUT /api/v1/admin/issues/{id} — Update issue */
    @PutMapping("/issues/{id}")
    public ResponseEntity<IssueDTO> updateIssue(@PathVariable Long id, @RequestBody IssueDTO dto) {
        return ResponseEntity.ok(issueService.updateIssue(id, dto));
    }

    /** PUT /api/v1/admin/issues/{id}/set-current — Set issue as active current */
    @PutMapping("/issues/{id}/set-current")
    public ResponseEntity<IssueDTO> setCurrentIssue(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.setCurrentIssue(id));
    }

    /** POST /api/v1/admin/board-members — Add a board member */
    @PostMapping("/board-members")
    public ResponseEntity<BoardMemberDTO> createBoardMember(@RequestBody BoardMemberDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardMemberService.createMember(dto));
    }

    /** PUT /api/v1/admin/board-members/{id} — Update board member */
    @PutMapping("/board-members/{id}")
    public ResponseEntity<BoardMemberDTO> updateBoardMember(@PathVariable Long id, @RequestBody BoardMemberDTO dto) {
        return ResponseEntity.ok(boardMemberService.updateMember(id, dto));
    }

    /** DELETE /api/v1/admin/board-members/{id} — Delete board member */
    @DeleteMapping("/board-members/{id}")
    public ResponseEntity<Map<String, String>> deleteBoardMember(@PathVariable Long id) {
        boardMemberService.deleteMember(id);
        return ResponseEntity.ok(Map.of("message", "Board member removed successfully."));
    }
}
