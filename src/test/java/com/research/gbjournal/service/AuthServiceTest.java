package com.research.gbjournal.service;

import com.research.gbjournal.dto.auth.AuthResponse;
import com.research.gbjournal.dto.auth.LoginRequest;
import com.research.gbjournal.dto.auth.RegisterRequest;
import com.research.gbjournal.entity.RefreshToken;
import com.research.gbjournal.entity.User;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.repository.UserRepository;
import com.research.gbjournal.security.JwtProperties;
import com.research.gbjournal.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Test Author")
                .email("test@gonouniversity.edu.bd")
                .password("encoded_pass")
                .role(User.Role.AUTHOR)
                .enabled(true)
                .build();
    }

    @Test
    void login_Successful() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@gonouniversity.edu.bd");
        loginRequest.setPassword("demopass");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtProvider.generateAccessToken(auth)).thenReturn("sample_access_token");
        when(userRepository.findByEmailIgnoreCase("test@gonouniversity.edu.bd")).thenReturn(Optional.of(sampleUser));
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn(
                RefreshToken.builder().token("sample_refresh_token").build());
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("sample_access_token", response.getAccessToken());
        assertEquals("sample_refresh_token", response.getRefreshToken());
        assertEquals("author", response.getUser().getRole());
    }

    @Test
    void login_InvalidCredentials_ThrowsBadRequestException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@gonouniversity.edu.bd");
        loginRequest.setPassword("wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }

    @Test
    void register_DuplicateEmail_ThrowsBadRequestException() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("Duplicate User");
        registerRequest.setEmail("test@gonouniversity.edu.bd");
        registerRequest.setPassword("Password123!");

        when(userRepository.existsByEmailIgnoreCase("test@gonouniversity.edu.bd")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
    }
}
