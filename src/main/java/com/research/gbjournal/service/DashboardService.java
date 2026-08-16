package com.research.gbjournal.service;

import com.research.gbjournal.dto.admin.DashboardStatsDTO;
import com.research.gbjournal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubmissionRepository submissionRepository;
    private final ArticleRepository articleRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats() {
        return DashboardStatsDTO.builder()
                .activeSubmissions(submissionRepository.countActiveSubmissions())
                .underReview(submissionRepository.countUnderReview())
                .accepted(submissionRepository.countAccepted())
                .publishedArticles(articleRepository.count())
                .activeReviewers(reviewAssignmentRepository.countActiveReviewers())
                .publishedIssues(issueRepository.count())
                .registeredUsers(userRepository.count())
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.List<com.research.gbjournal.dto.auth.AuthResponse.UserInfo> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> toUserInfo(u))
                .toList();
    }

    @Transactional
    public com.research.gbjournal.dto.auth.AuthResponse.UserInfo updateUserRole(Long userId, String roleStr) {
        com.research.gbjournal.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.research.gbjournal.exception.ResourceNotFoundException("User", "id", userId));
        String formattedRole = roleStr.toUpperCase().replace('-', '_');
        user.setRole(com.research.gbjournal.entity.User.Role.valueOf(formattedRole));
        userRepository.save(user);
        return toUserInfo(user);
    }

    @Transactional
    public com.research.gbjournal.dto.auth.AuthResponse.UserInfo updateUserStatus(Long userId, boolean enabled) {
        com.research.gbjournal.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.research.gbjournal.exception.ResourceNotFoundException("User", "id", userId));
        user.setEnabled(enabled);
        userRepository.save(user);
        return toUserInfo(user);
    }

    private com.research.gbjournal.dto.auth.AuthResponse.UserInfo toUserInfo(com.research.gbjournal.entity.User u) {
        return com.research.gbjournal.dto.auth.AuthResponse.UserInfo.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole().name().toLowerCase().replace('_', '-'))
                .title(u.getTitle())
                .department(u.getDepartment())
                .institution(u.getInstitution())
                .avatarUrl(u.getAvatarUrl())
                .emailVerified(u.isEmailVerified())
                .build();
    }
}
