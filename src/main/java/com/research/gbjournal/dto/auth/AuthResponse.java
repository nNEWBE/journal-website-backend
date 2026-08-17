package com.research.gbjournal.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;   // seconds
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String title;
        private String department;
        private String institution;
        private String avatarUrl;
        private boolean emailVerified;
        private boolean enabled;

        public String getName() {
            return fullName;
        }

        public String getAvatar() {
            return avatarUrl;
        }
    }
}
