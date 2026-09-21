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
        String queryRole = role;
        if ((queryRole == null || queryRole.isBlank()) && userDetails != null) {
            queryRole = userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                    .findFirst()
                    .orElse(null);
        }
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
