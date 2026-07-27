package com.research.gbjournal.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /** HMAC-SHA256 secret — must be at least 256-bit (32 chars) */
    private String secret;

    /** Access token TTL in milliseconds (default 15 minutes) */
    private long accessTokenExpirationMs = 900_000L;

    /** Refresh token TTL in milliseconds (default 7 days) */
    private long refreshTokenExpirationMs = 604_800_000L;
}
