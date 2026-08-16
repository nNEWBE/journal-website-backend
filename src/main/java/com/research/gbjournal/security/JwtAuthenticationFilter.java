package com.research.gbjournal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JWT_COOKIE_NAME = "gb_access_token";

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
                String email = jwtProvider.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Build authentication token — no WebAuthenticationDetailsSource (deprecated in 6.5+)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Do not let any exception break the filter chain; unauthenticated request will be
            // rejected downstream by the security configuration.
            log.debug("JWT filter encountered an error: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Attempts to resolve the JWT from:
     * 1. Authorization: Bearer {@code <token>} header
     * 2. gb_access_token HTTP-only cookie (for browser / Next.js SSR clients)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Header-based (preferred for API clients / mobile)
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // 2. Cookie-based (browser / Next.js SSR)
        if (request.getCookies() != null) {
            Optional<Cookie> cookie = Arrays.stream(request.getCookies())
                    .filter(c -> JWT_COOKIE_NAME.equals(c.getName()))
                    .findFirst();
            if (cookie.isPresent()) {
                return cookie.get().getValue();
            }
        }

        return null;
    }
}
