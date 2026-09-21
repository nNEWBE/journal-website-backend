package com.research.gbjournal.service;

import com.research.gbjournal.config.MailProperties;
import com.research.gbjournal.dto.auth.*;
import com.research.gbjournal.entity.PasswordResetToken;
import com.research.gbjournal.entity.RefreshToken;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.PasswordResetTokenRepository;
import com.research.gbjournal.repository.UserRepository;
import com.research.gbjournal.security.JwtProperties;
import com.research.gbjournal.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailProperties mailProperties;

    // ===== Login =====

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()));
        } catch (org.springframework.security.authentication.DisabledException ex) {
            throw new BadRequestException("This account has been disabled. Please contact administration.");
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password.");
        }

        String accessToken = jwtProvider.generateAccessToken(authentication);

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }

    // ===== Register =====

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.AUTHOR) // New users default to AUTHOR role
                .institution(request.getInstitution())
                .department(request.getDepartment())
                .country(request.getCountry())
                .orcid(request.getOrcid())
                .researchInterests(request.getResearchInterests())
                .enabled(true)
                .emailVerified(false)
                .build();

        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(email);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("New user registered: {}", email);

        // Dispatch Welcome Email asynchronously
        try {
            java.util.Map<String, Object> emailVars = new java.util.HashMap<>();
            emailVars.put("fullName", user.getFullName());
            emailVars.put("email", user.getEmail());
            emailVars.put("role", user.getRole().name().replace('_', ' '));
            emailVars.put("institution", user.getInstitution() != null ? user.getInstitution() : "");
            emailVars.put("orcid", user.getOrcid() != null ? user.getOrcid() : "");
            emailVars.put("registeredAt", java.time.LocalDate.now().toString());
            emailService.sendHtml(
                    user.getEmail(),
                    "Welcome to Gono Bishwabidyalay Journal — Your Academic Account is Ready",
                    "email/account-welcome",
                    emailVars);
        } catch (Exception ex) {
            log.warn("Failed to dispatch welcome email to {}: {}", user.getEmail(), ex.getMessage());
        }

        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }

    // ===== Refresh Token =====

    @Transactional
    public AuthResponse refresh(TokenRefreshRequest request) {
        RefreshToken oldToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        User user = oldToken.getUser();

        String newAccessToken = jwtProvider.generateAccessToken(user.getEmail());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(newAccessToken, newRefreshToken.getToken(), user);
    }

    // ===== Logout =====

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        refreshTokenService.revokeAllTokensForUser(user);
        log.info("User logged out: {}", email);
    }

    // ===== Get Me =====

    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getMe(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapUserInfo(user);
    }

    // ===== Update Profile =====

    @Transactional
    public AuthResponse.UserInfo updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (StringUtils.hasText(request.getFullName()))
            user.setFullName(request.getFullName().trim());
        if (StringUtils.hasText(request.getInstitution()))
            user.setInstitution(request.getInstitution());
        if (StringUtils.hasText(request.getDepartment()))
            user.setDepartment(request.getDepartment());
        if (StringUtils.hasText(request.getCountry()))
            user.setCountry(request.getCountry());
        if (StringUtils.hasText(request.getOrcid()))
            user.setOrcid(request.getOrcid());
        if (StringUtils.hasText(request.getResearchInterests()))
            user.setResearchInterests(request.getResearchInterests());
        if (request.getAvatarUrl() != null) {
            String oldAvatarUrl = user.getAvatarUrl();
            if (StringUtils.hasText(oldAvatarUrl) && !oldAvatarUrl.equals(request.getAvatarUrl())) {
                cloudinaryService.deleteByUrl(oldAvatarUrl);
            }
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.save(user);
        return mapUserInfo(user);
    }

    // ===== Change Password =====

    @Transactional
    public void changePassword(String email, com.research.gbjournal.dto.auth.ChangePasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ===== Upload Avatar to Cloudinary =====

    @Transactional
    public AuthResponse.UserInfo uploadAvatar(String email, org.springframework.web.multipart.MultipartFile file) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String oldAvatarUrl = user.getAvatarUrl();

        java.util.Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, "gbjournal/avatars");
        String secureUrl = (String) uploadResult.get("secure_url");
        if (StringUtils.hasText(secureUrl)) {
            // Remove previous avatar from Cloudinary if it exists
            if (StringUtils.hasText(oldAvatarUrl) && !oldAvatarUrl.equals(secureUrl)) {
                cloudinaryService.deleteByUrl(oldAvatarUrl);
            }
            user.setAvatarUrl(secureUrl);
            userRepository.save(user);
            log.info("Updated avatar in Cloudinary for user: {}, url: {}", email, secureUrl);
        }

        return mapUserInfo(user);
    }

    // ===== Helpers =====

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000)
                .user(mapUserInfo(user))
                .build();
    }

    private AuthResponse.UserInfo mapUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name().toLowerCase().replace('_', '-'))
                .title(user.getTitle())
                .department(user.getDepartment())
                .institution(user.getInstitution())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    // ===== Request Password Reset =====

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        java.util.Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty()) {
            // Anti-enumeration: return silently without disclosing whether account exists
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Invalidate/delete any previous active reset tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate secure UUID token
        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(30)))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        String frontendUrl = (mailProperties.getJournalUrl() != null && !mailProperties.getJournalUrl().isBlank())
                ? mailProperties.getJournalUrl().replaceAll("/+$", "")
                : "http://localhost:3000";
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        log.info("Generated password reset link for {}: {}", email, resetUrl);

        try {
            java.util.Map<String, Object> emailVars = new java.util.HashMap<>();
            emailVars.put("fullName", user.getFullName() != null ? user.getFullName() : "Scholar");
            emailVars.put("email", user.getEmail());
            emailVars.put("resetUrl", resetUrl);
            emailVars.put("validityMinutes", 30);
            emailService.sendHtml(
                    user.getEmail(),
                    "Reset Your Password — Gono Bishwabidyalay Journal",
                    "email/password-reset",
                    emailVars);
        } catch (Exception ex) {
            log.warn("Failed to dispatch password reset email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    // ===== Validate Password Reset Token =====

    @Transactional(readOnly = true)
    public ResetTokenValidationResponse validatePasswordResetToken(String token) {
        if (!StringUtils.hasText(token)) {
            return ResetTokenValidationResponse.builder()
                    .valid(false)
                    .message("Password reset token is missing.")
                    .build();
        }

        java.util.Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            return ResetTokenValidationResponse.builder()
                    .valid(false)
                    .message("The password reset link is invalid or has expired.")
                    .build();
        }

        User user = tokenOpt.get().getUser();
        String masked = maskEmail(user.getEmail());

        return ResetTokenValidationResponse.builder()
                .valid(true)
                .email(masked)
                .message("Token is valid.")
                .build();
    }

    // ===== Reset Password =====

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("The password reset link is invalid or has expired. Please request a new one."));

        if (!token.isValid()) {
            throw new BadRequestException("The password reset link is invalid or has expired. Please request a new one.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Revoke all existing refresh tokens for security
        refreshTokenService.revokeAllTokensForUser(user);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
