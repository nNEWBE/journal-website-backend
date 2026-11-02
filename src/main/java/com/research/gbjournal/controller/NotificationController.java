package com.research.gbjournal.controller;

import com.research.gbjournal.entity.Notification;
import com.research.gbjournal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false) String role,
            @AuthenticationPrincipal UserDetails userDetails) {
        String ownRole = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .findFirst()
                .orElse("author");
        boolean isAdmin = ownRole.equals("admin") || ownRole.equals("super_admin");

        // Only administrators may view another role's feed; everyone else always gets their own,
        // otherwise any author could read editor/admin notifications by passing ?role=admin.
        String queryRole = isAdmin && role != null && !role.isBlank() ? role : ownRole;
        return ResponseEntity.ok(notificationService.getNotificationsForRole(queryRole));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }

    @PatchMapping("/{id}/unread")
    public ResponseEntity<Map<String, String>> markAsUnread(@PathVariable Long id) {
        notificationService.markAsUnread(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as unread."));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }
}
