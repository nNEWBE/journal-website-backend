package com.research.gbjournal.service;

import com.research.gbjournal.entity.Notification;
import com.research.gbjournal.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(String title, String message, String type, String targetRoles, String link) {
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .type(type != null ? type : "system")
                .targetRoles(targetRoles)
                .link(link)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created site notification [{}]: {}", saved.getId(), title);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForRole(String role) {
        List<Notification> all = notificationRepository.findTop50ByOrderByCreatedAtDesc();
        if (role == null || role.isBlank()) {
            return all;
        }

        String normalizedRole = role.toLowerCase().replace("-", "_").trim();
        return all.stream().filter(n -> {
            if (n.getTargetRoles() == null || n.getTargetRoles().isBlank()) {
                return true;
            }
            return Arrays.stream(n.getTargetRoles().split(","))
                    .map(String::trim)
                    .anyMatch(r -> r.equalsIgnoreCase(normalizedRole) ||
                                   r.equalsIgnoreCase(role) ||
                                   (role.equalsIgnoreCase("super-admin") && (r.equalsIgnoreCase("admin") || r.equalsIgnoreCase("editor"))));
        }).toList();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id);
    }

    @Transactional
    public void markAsUnread(Long id) {
        notificationRepository.markAsUnread(id);
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead();
    }
}
