package com.research.gbjournal.service;

import com.research.gbjournal.dto.admin.CreateUserRequest;
import com.research.gbjournal.dto.admin.DashboardStatsDTO;
import com.research.gbjournal.dto.admin.SendMailRequest;
import com.research.gbjournal.dto.auth.AuthResponse;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubmissionRepository submissionRepository;
    private final ArticleRepository articleRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final RefreshTokenService refreshTokenService;

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
    private static final String TEMP_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    @Cacheable(value = "dashboard-stats")
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

    @Cacheable(value = "admin-users")
    @Transactional(readOnly = true)
    public List<AuthResponse.UserInfo> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(Objects::nonNull)
                .map(u -> toUserInfo(u))
                .toList();
    }

    @CacheEvict(value = {"admin-users", "dashboard-stats"}, allEntries = true)
    @Transactional
    public AuthResponse.UserInfo createUser(CreateUserRequest req, String actorEmail) {
        User actor = requireActor(actorEmail);
        if (userRepository.existsByEmailIgnoreCase(req.getEmail().trim())) {
            throw new BadRequestException("A user with this email address already exists.");
        }

        String rawPassword = req.getPassword() != null && !req.getPassword().isBlank()
                ? req.getPassword()
                : generateTempPassword();

        String roleStr = req.getRole() != null ? req.getRole().toUpperCase().replace('-', '_') : "AUTHOR";
        User.Role role;
        try {
            role = User.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            role = User.Role.AUTHOR;
        }
        assertCanAssignRole(actor, role);

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(req.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .title(req.getTitle() != null ? req.getTitle().trim() : "Scholar")
                .department(req.getDepartment())
                .institution(req.getInstitution() != null ? req.getInstitution() : "Gono Bishwabidyalay")
                .orcid(req.getOrcid())
                .avatarUrl(req.getAvatarUrl() != null && !req.getAvatarUrl().isBlank() ? req.getAvatarUrl().trim() : null)
                .emailVerified(true)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created new user: {} with role {}", saved.getEmail(), saved.getRole());

        // Dispatch welcome invitation email asynchronously
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("fullName", saved.getFullName());
            vars.put("email", saved.getEmail());
            vars.put("tempPassword", rawPassword);
            vars.put("role", saved.getRole().name().replace('_', ' '));
            vars.put("institution", saved.getInstitution() != null ? saved.getInstitution() : "");
            vars.put("orcid", saved.getOrcid() != null ? saved.getOrcid() : "");
            vars.put("registeredAt", java.time.LocalDate.now().toString());
            emailService.sendHtml(
                    saved.getEmail(),
                    "Welcome to Gono Bishwabidyalay Journal Portal — Account Created",
                    "email/account-welcome",
                    vars);
        } catch (Exception ex) {
            log.warn("Failed to dispatch welcome email to {}: {}", saved.getEmail(), ex.getMessage());
        }

        return toUserInfo(saved);
    }

    @CacheEvict(value = {"admin-users", "dashboard-stats", "userDetails"}, allEntries = true)
    @Transactional
    public void deleteUser(Long userId, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new BadRequestException("You cannot delete your own active administrator account.");
        }
        assertCanManage(requireActor(currentAdminEmail), user);

        userRepository.delete(user);
        log.info("Admin {} deleted user ID {}", currentAdminEmail, userId);
    }

    @CacheEvict(value = {"admin-users", "userDetails"}, allEntries = true)
    @Transactional
    public AuthResponse.UserInfo updateUser(Long userId, com.research.gbjournal.dto.admin.UpdateUserRequest req,
                                            String actorEmail) {
        User actor = requireActor(actorEmail);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        assertCanManage(actor, user);
        boolean revokeSessions = false;

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.findByEmailIgnoreCase(newEmail).isPresent()) {
                    throw new BadRequestException("An account with email '" + newEmail + "' already exists.");
                }
                user.setEmail(newEmail);
            }
        }
        if (req.getSecondaryEmail() != null) {
            user.setSecondaryEmail(req.getSecondaryEmail().trim().isEmpty() ? null : req.getSecondaryEmail().trim().toLowerCase());
        }
        if (req.getTitle() != null) {
            user.setTitle(req.getTitle().trim().isEmpty() ? null : req.getTitle().trim());
        }
        if (req.getDepartment() != null) {
            user.setDepartment(req.getDepartment().trim().isEmpty() ? null : req.getDepartment().trim());
        }
        if (req.getInstitution() != null) {
            user.setInstitution(req.getInstitution().trim().isEmpty() ? null : req.getInstitution().trim());
        }
        if (req.getCountry() != null) {
            user.setCountry(req.getCountry().trim().isEmpty() ? null : req.getCountry().trim());
        }
        if (req.getOrcid() != null) {
            user.setOrcid(req.getOrcid().trim().isEmpty() ? null : req.getOrcid().trim());
        }
        if (req.getResearchInterests() != null) {
            user.setResearchInterests(req.getResearchInterests().trim().isEmpty() ? null : req.getResearchInterests().trim());
        }
        if (req.getAvatarUrl() != null) {
            String newAvatar = req.getAvatarUrl().trim().isEmpty() ? null : req.getAvatarUrl().trim();
            String oldAvatar = user.getAvatarUrl();
            if (oldAvatar != null && !oldAvatar.equals(newAvatar)) {
                cloudinaryService.deleteByUrl(oldAvatar);
            }
            user.setAvatarUrl(newAvatar);
        }
        if (req.getRole() != null && !req.getRole().isBlank()) {
            User.Role newRole = parseRole(req.getRole());
            if (newRole != user.getRole()) {
                assertCanAssignRole(actor, newRole);
                assertNotSelf(actor, user, "change your own role");
                user.setRole(newRole);
                revokeSessions = true;
            }
        }
        if (req.getEnabled() != null) {
            if (!req.getEnabled()) {
                assertNotSelf(actor, user, "disable your own account");
                revokeSessions = true;
            }
            user.setEnabled(req.getEnabled());
        }
        if (req.getEmailVerified() != null) {
            user.setEmailVerified(req.getEmailVerified());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().trim().length() < 8) {
                throw new BadRequestException("Password must be at least 8 characters.");
            }
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
            revokeSessions = true;
        }

        userRepository.save(user);
        if (revokeSessions) {
            refreshTokenService.revokeAllTokensForUser(user);
        }
        log.info("Admin updated user ID {}: email={}, role={}, enabled={}", user.getId(), user.getEmail(), user.getRole(), user.isEnabled());
        return toUserInfo(user);
    }

    @CacheEvict(value = {"admin-users", "userDetails"}, allEntries = true)
    @Transactional
    public AuthResponse.UserInfo updateUserRole(Long userId, String roleStr, String actorEmail) {
        User actor = requireActor(actorEmail);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        assertCanManage(actor, user);
        User.Role newRole = parseRole(roleStr);
        if (newRole != user.getRole()) {
            assertCanAssignRole(actor, newRole);
            assertNotSelf(actor, user, "change your own role");
            user.setRole(newRole);
            userRepository.save(user);
            refreshTokenService.revokeAllTokensForUser(user);
        }
        return toUserInfo(user);
    }

    @CacheEvict(value = {"admin-users", "userDetails"}, allEntries = true)
    @Transactional
    public AuthResponse.UserInfo updateUserStatus(Long userId, boolean enabled, String actorEmail) {
        User actor = requireActor(actorEmail);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        assertCanManage(actor, user);
        if (!enabled) {
            assertNotSelf(actor, user, "disable your own account");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        if (!enabled) {
            refreshTokenService.revokeAllTokensForUser(user);
        }
        return toUserInfo(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sendAdminMail(SendMailRequest req, String currentAdminEmail) {
        List<String> recipients = new ArrayList<>();

        if ("INDIVIDUAL".equalsIgnoreCase(req.getAudience()) && req.getRecipientEmail() != null) {
            String[] parts = req.getRecipientEmail().split("[,;]");
            for (String p : parts) {
                if (!p.trim().isEmpty())
                    recipients.add(p.trim());
            }
        } else if ("ALL_AUTHORS".equalsIgnoreCase(req.getAudience())) {
            recipients = userRepository.findAll().stream()
                    .filter(u -> u != null && u.getRole() == User.Role.AUTHOR && u.getEmail() != null)
                    .map(u -> u.getEmail())
                    .toList();
        } else if ("ALL_REVIEWERS".equalsIgnoreCase(req.getAudience())) {
            recipients = userRepository.findAll().stream()
                    .filter(u -> u != null && u.getRole() == User.Role.REVIEWER && u.getEmail() != null)
                    .map(u -> u.getEmail())
                    .toList();
        } else if ("ALL_EDITORS".equalsIgnoreCase(req.getAudience())) {
            recipients = userRepository.findAll().stream()
                    .filter(u -> u != null && (u.getRole() == User.Role.EDITOR || u.getRole() == User.Role.ADMIN
                            || u.getRole() == User.Role.SUPER_ADMIN) && u.getEmail() != null)
                    .map(u -> u.getEmail())
                    .toList();
        } else if ("ALL_USERS".equalsIgnoreCase(req.getAudience())) {
            recipients = userRepository.findAll().stream()
                    .filter(u -> u != null && u.getEmail() != null)
                    .map(u -> u.getEmail())
                    .toList();
        }

        if (recipients.isEmpty()) {
            throw new BadRequestException("No recipients found matching the target audience: " + req.getAudience());
        }

        int sentCount = 0;
        for (String email : recipients) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("authorName", email.split("@")[0]);
            vars.put("subject", req.getSubject());
            vars.put("messageBody", req.getMessageBody());
            vars.put("senderAdmin", currentAdminEmail);
            emailService.sendHtml(email, req.getSubject(), "email/decision-letter", vars);
            sentCount++;
        }

        log.info("Admin {} dispatched mail '{}' to {} recipient(s)", currentAdminEmail, req.getSubject(), sentCount);
        return Map.of(
                "success", true,
                "sentCount", sentCount,
                "audience", req.getAudience(),
                "subject", req.getSubject());
    }

    // ===== Authorization helpers =====

    private User requireActor(String actorEmail) {
        return userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new AccessDeniedException("Unknown administrator."));
    }

    /** Only a SUPER_ADMIN may modify or delete another SUPER_ADMIN account. */
    private void assertCanManage(User actor, User target) {
        if (target.getRole() == User.Role.SUPER_ADMIN && actor.getRole() != User.Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only a super administrator can modify a super administrator account.");
        }
    }

    /** Only a SUPER_ADMIN may grant the SUPER_ADMIN role (prevents admin -> super-admin escalation). */
    private void assertCanAssignRole(User actor, User.Role role) {
        if (role == User.Role.SUPER_ADMIN && actor.getRole() != User.Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only a super administrator can grant the super administrator role.");
        }
    }

    private void assertNotSelf(User actor, User target, String action) {
        if (actor.getId() != null && actor.getId().equals(target.getId())) {
            throw new BadRequestException("You cannot " + action + ".");
        }
    }

    private User.Role parseRole(String roleStr) {
        try {
            return User.Role.valueOf(roleStr.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleStr);
        }
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder("Gbj-");
        for (int i = 0; i < 14; i++) {
            sb.append(TEMP_PASSWORD_ALPHABET.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return sb.append('9').toString();
    }

    private AuthResponse.UserInfo toUserInfo(User u) {
        return AuthResponse.UserInfo.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .secondaryEmail(u.getSecondaryEmail())
                .role(u.getRole().name().toLowerCase().replace('_', '-'))
                .title(u.getTitle())
                .department(u.getDepartment())
                .institution(u.getInstitution())
                .country(u.getCountry())
                .orcid(u.getOrcid())
                .researchInterests(u.getResearchInterests())
                .avatarUrl(u.getAvatarUrl())
                .emailVerified(u.isEmailVerified())
                .enabled(u.isEnabled())
                .build();
    }
}
