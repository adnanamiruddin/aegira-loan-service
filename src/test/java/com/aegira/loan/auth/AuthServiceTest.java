package com.aegira.loan.auth;

import com.aegira.loan.auth.dto.LoginRequest;
import com.aegira.loan.auth.dto.LoginResponse;
import com.aegira.loan.common.exception.BadRequestException;
import com.aegira.loan.common.security.SecurityUtil;
import com.aegira.loan.user.entity.Role;
import com.aegira.loan.user.entity.User;
import com.aegira.loan.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private SecurityUtil securityUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Dependency di luar AuthService dibuat mock.
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        securityUtil = mock(SecurityUtil.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, securityUtil);
    }

    @Test
    void login_shouldReturnTokenWhenEmailAndPasswordAreValid() {
        // Arrange
        User user = user();
        LoginRequest request = loginRequest(user.getEmail(), "password123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generate(user)).thenReturn("jwt-token");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertEquals("jwt-token", response.getToken());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getRole(), response.getRole());
    }

    @Test
    void login_shouldThrowErrorWhenEmailIsNotRegistered() {
        // Arrange
        LoginRequest request = loginRequest("unknown@aegira.com", "password123");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(BadRequestException.class, () -> authService.login(request));
        verify(jwtTokenProvider, never()).generate(any(User.class));
    }

    @Test
    void login_shouldThrowErrorWhenPasswordIsInvalid() {
        // Arrange
        User user = user();
        LoginRequest request = loginRequest(user.getEmail(), "wrong-password");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        // Act and Assert
        assertThrows(BadRequestException.class, () -> authService.login(request));
        verify(jwtTokenProvider, never()).generate(any(User.class));
    }

    @Test
    void me_shouldReturnCurrentUserWithoutToken() {
        // Arrange
        User user = user();
        when(securityUtil.currentUser()).thenReturn(user);

        // Act
        LoginResponse response = authService.me();

        // Assert
        assertNull(response.getToken());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getRole(), response.getRole());
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("agent@aegira.com");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.AGENT);
        return user;
    }
}
