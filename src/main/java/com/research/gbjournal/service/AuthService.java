package com.research.gbjournal.service;

import com.research.gbjournal.dto.auth.*;
import com.research.gbjournal.entity.RefreshToken;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.UserRepository;
import com.research.gbjournal.security.JwtProperties;
import com.research.gbjournal.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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

    // ===== Login =====

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()));
        } catch (BadCredentialsException ex) {
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

        if (StringUtils.hasText(request.getFullName()))       user.setFullName(request.getFullName().trim());
        if (StringUtils.hasText(request.getInstitution()))    user.setInstitution(request.getInstitution());
        if (StringUtils.hasText(request.getDepartment()))     user.setDepartment(request.getDepartment());
        if (StringUtils.hasText(request.getCountry()))        user.setCountry(request.getCountry());
        if (StringUtils.hasText(request.getOrcid()))          user.setOrcid(request.getOrcid());
        if (StringUtils.hasText(request.getResearchInterests())) user.setResearchInterests(request.getResearchInterests());
        if (StringUtils.hasText(request.getTitle()))          user.setTitle(request.getTitle());
        if (StringUtils.hasText(request.getAvatarUrl()))      user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);
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
}
