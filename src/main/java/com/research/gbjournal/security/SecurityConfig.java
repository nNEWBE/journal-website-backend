package com.research.gbjournal.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final AccessDeniedHandlerJwt accessDeniedHandlerJwt;

    @Value("${app.cors-allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    // ===== Password Encoder =====

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ===== Authentication Provider =====

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 6.5+: constructor requires UserDetailsService; password encoder is a setter
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ===== Security Filter Chain =====

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless JWT API, no cookie-based session
            .csrf(csrf -> csrf.disable())

            // CORS — allow Next.js frontend
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless sessions — never create HttpSession
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Custom JSON error responses
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPointJwt)
                .accessDeniedHandler(accessDeniedHandlerJwt))

            // Route-level authorization
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh").permitAll()

                // H2 console (dev only — safe because it runs in-memory)
                .requestMatchers("/h2-console/**").permitAll()

                // Public article & issue discovery (read-only GET)
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/articles",
                    "/api/v1/articles/**",
                    "/api/v1/issues",
                    "/api/v1/issues/**",
                    "/api/v1/editorial-board",
                    "/api/v1/topics",
                    "/api/v1/article-types",
                    "/api/v1/files/**").permitAll()

                // Profile & authenticated user actions
                .requestMatchers(
                    "/api/v1/auth/me",
                    "/api/v1/auth/logout",
                    "/api/v1/auth/profile").authenticated()

                // File upload — any authenticated user
                .requestMatchers(HttpMethod.POST, "/api/v1/files/upload-image").authenticated()

                // Author — any authenticated user can submit manuscripts
                .requestMatchers("/api/v1/submissions/**").authenticated()

                // Reviewer area — reviewer and above
                .requestMatchers("/api/v1/reviewer/**")
                    .hasAnyRole("REVIEWER", "EDITOR", "ADMIN", "SUPER_ADMIN")

                // Editor area
                .requestMatchers("/api/v1/editor/**")
                    .hasAnyRole("EDITOR", "ADMIN", "SUPER_ADMIN")

                // Admin area
                .requestMatchers("/api/v1/admin/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")

                .anyRequest().authenticated()
            )

            // Register the JWT auth provider
            .authenticationProvider(authenticationProvider())

            // Place JWT filter before the default username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Allow H2 console iframes — dev only; same-origin policy
            .headers(headers ->
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    // ===== CORS =====

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(corsAllowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
