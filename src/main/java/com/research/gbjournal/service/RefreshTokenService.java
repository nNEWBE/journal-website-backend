package com.research.gbjournal.service;

import com.research.gbjournal.entity.RefreshToken;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.repository.RefreshTokenRepository;
import com.research.gbjournal.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * Creates a new opaque refresh token for the given user.
     * All previous tokens for this user remain until they expire or are used — they are revoked on logout.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * Validates the refresh token and marks it as revoked (single-use rotation).
     *
     * @param rawToken the raw token string from the client
     * @return the valid, un-revoked RefreshToken entity
     * @throws BadRequestException if token is not found, expired, or already revoked
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token."));

        if (!token.isValid()) {
            // If already revoked, this could be a token reuse attack — revoke all tokens for the user
            if (token.isRevoked()) {
                log.warn("Refresh token reuse detected for user {}. Revoking all tokens.", token.getUser().getEmail());
                refreshTokenRepository.revokeAllTokensForUser(token.getUser());
                throw new BadRequestException("Refresh token reuse detected. Please log in again.");
            }
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }

        // Mark the current token as revoked (rotation — each token is single-use)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return token;
    }

    /**
     * Revokes all active refresh tokens for the given user (logout).
     */
    @Transactional
    public void revokeAllTokensForUser(User user) {
        refreshTokenRepository.revokeAllTokensForUser(user);
        log.debug("All refresh tokens revoked for user: {}", user.getEmail());
    }

    /**
     * Scheduled cleanup of expired and revoked tokens (called from DataInitializer or a scheduler).
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked();
        log.info("Cleaned up {} expired/revoked refresh tokens", deleted);
        return deleted;
    }
}
