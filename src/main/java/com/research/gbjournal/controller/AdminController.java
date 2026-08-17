package com.research.gbjournal.controller;

import com.research.gbjournal.dto.admin.AuditLogDTO;
import com.research.gbjournal.dto.admin.CreateUserRequest;
import com.research.gbjournal.dto.admin.DashboardStatsDTO;
import com.research.gbjournal.dto.admin.SendMailRequest;
import com.research.gbjournal.dto.auth.AuthResponse;
import com.research.gbjournal.dto.board.BoardMemberDTO;
import com.research.gbjournal.dto.issue.IssueDTO;
import com.research.gbjournal.dto.submission.SubmissionResponseDTO;
import com.research.gbjournal.service.BoardMemberService;
import com.research.gbjournal.service.DashboardService;
import com.research.gbjournal.service.IssueService;
import com.research.gbjournal.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final SubmissionService submissionService;
    private final com.research.gbjournal.service.PageContentService pageContentService;

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

    /** POST /api/v1/admin/users — Create / invite a user */
    @PostMapping("/users")
    public ResponseEntity<AuthResponse.UserInfo> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboardService.createUser(req));
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

    /** DELETE /api/v1/admin/users/{id} — Delete user */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@gbjournal.org";
        dashboardService.deleteUser(id, adminEmail);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    /** GET /api/v1/admin/submissions — List all submissions with filters & pagination */
    @GetMapping("/submissions")
    public ResponseEntity<Page<SubmissionResponseDTO>> listAllSubmissions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(submissionService.getAllSubmissions(status, type, page, size));
    }

    /** POST /api/v1/admin/mail/send — Send targeted email or broadcast */
    @PostMapping("/mail/send")
    public ResponseEntity<Map<String, Object>> sendMail(
            @Valid @RequestBody SendMailRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@gbjournal.org";
        return ResponseEntity.ok(dashboardService.sendAdminMail(req, adminEmail));
    }

    /** GET /api/v1/admin/mail/templates — Predefined email templates */
    @GetMapping("/mail/templates")
    public ResponseEntity<List<Map<String, String>>> getMailTemplates() {
        List<Map<String, String>> templates = List.of(
                Map.of(
                        "key", "call-for-papers",
                        "name", "Call for Papers — Upcoming Volume",
                        "subject", "Call for Papers: Gono Bishwabidyalay Journal of Science & Technology",
                        "body", "Dear Scholars,\n\nWe are pleased to invite original research papers and review articles for our upcoming volume. Authors are encouraged to submit manuscripts covering Multidisciplinary Sciences, Health & Pharmacy, Engineering, and Social Sciences.\n\nBest regards,\nEditorial Board\nGono Bishwabidyalay Journal"
                ),
                Map.of(
                        "key", "review-reminder",
                        "name", "Peer Review Reminder Notice",
                        "subject", "Friendly Reminder: Peer Review Due Soon — GB Journal",
                        "body", "Dear Reviewer,\n\nThis is a polite reminder regarding the manuscript assigned to you for double-blind peer review. We kindly request you to complete your evaluation by the due date.\n\nThank you for supporting our academic peer-review standards.\n\nBest regards,\nEditorial Secretariat"
                ),
                Map.of(
                        "key", "system-announcement",
                        "name", "General System Broadcast",
                        "subject", "Important Announcement from GB Journal Administration",
                        "body", "Dear Academic Community,\n\nPlease be informed of scheduled platform maintenance and workflow enhancements across the GB Journal Management Portal.\n\nFor any inquiries, please reach out to the editorial office.\n\nSincerely,\nJournal Administration"
                )
        );
        return ResponseEntity.ok(templates);
    }

    /** GET /api/v1/admin/audit-logs — System activity logs */
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogs() {
        return ResponseEntity.ok(dashboardService.getAuditLogs());
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

    // ===== Page Content & CMS Endpoints =====

    /** GET /api/v1/admin/content/{pageKey} — List all sections for page (including drafts) */
    @GetMapping("/content/{pageKey}")
    public ResponseEntity<List<com.research.gbjournal.dto.content.PageContentDTO>> getAdminPageContent(@PathVariable String pageKey) {
        return ResponseEntity.ok(pageContentService.getAdminPageContent(pageKey));
    }

    /** GET /api/v1/admin/content/all — List all site content grouped */
    @GetMapping("/content/all")
    public ResponseEntity<Map<String, List<com.research.gbjournal.dto.content.PageContentDTO>>> getAdminAllContent() {
        return ResponseEntity.ok(pageContentService.getAllPagesContent());
    }

    /** PUT /api/v1/admin/content/{pageKey}/{sectionKey} — Update section */
    @PutMapping("/content/{pageKey}/{sectionKey}")
    public ResponseEntity<com.research.gbjournal.dto.content.PageContentDTO> updateSection(
            @PathVariable String pageKey,
            @PathVariable String sectionKey,
            @RequestBody com.research.gbjournal.dto.content.PageContentDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@gbjournal.org";
        return ResponseEntity.ok(pageContentService.updateSection(pageKey, sectionKey, dto, adminEmail));
    }

    /** POST /api/v1/admin/content/sections — Create new section */
    @PostMapping("/content/sections")
    public ResponseEntity<com.research.gbjournal.dto.content.PageContentDTO> createSection(
            @RequestBody com.research.gbjournal.dto.content.PageContentDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@gbjournal.org";
        return ResponseEntity.status(HttpStatus.CREATED).body(pageContentService.createSection(dto, adminEmail));
    }

    /** DELETE /api/v1/admin/content/sections/{id} — Delete section */
    @DeleteMapping("/content/sections/{id}")
    public ResponseEntity<Map<String, String>> deleteSection(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@gbjournal.org";
        pageContentService.deleteSection(id, adminEmail);
        return ResponseEntity.ok(Map.of("message", "Section removed successfully."));
    }

    /** POST /api/v1/admin/content/reset-defaults — Reset page or all content to defaults */
    @PostMapping("/content/reset-defaults")
    public ResponseEntity<Map<String, String>> resetDefaults(
            @RequestParam(required = false) String pageKey,
            @AuthenticationPrincipal UserDetails userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@gbjournal.org";
        pageContentService.resetDefaults(pageKey, adminEmail);
        return ResponseEntity.ok(Map.of("message", "Default content restored successfully."));
    }
}

