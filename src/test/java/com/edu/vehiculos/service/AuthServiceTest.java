package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.Role;
import com.edu.vehiculos.domain.User;
import com.edu.vehiculos.domain.dto.AuthResponse;
import com.edu.vehiculos.domain.dto.LoginRequest;
import com.edu.vehiculos.domain.dto.RegisterRequest;
import com.edu.vehiculos.repository.UserRepository;
import com.edu.vehiculos.repository.VehicleRepository;
import com.edu.vehiculos.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private VehicleRepository vehicleRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private AuthenticationManager authenticationManager;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        vehicleRepository = Mockito.mock(VehicleRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtUtils = Mockito.mock(JwtUtils.class);
        authenticationManager = Mockito.mock(AuthenticationManager.class);

        authService = new AuthService(
                userRepository,
                vehicleRepository,
                passwordEncoder,
                jwtUtils,
                authenticationManager
        );
    }

    @Test
    void testRegisterNewDriverSuccess() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Mario Silva")
                .email("mario@test.com")
                .password("pass123")
                .role("DRIVER")
                .phone("3001234567")
                .build();

        when(userRepository.existsByEmailIgnoreCase("mario@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");

        User savedUser = User.builder()
                .id(1L)
                .fullName("Mario Silva")
                .email("mario@test.com")
                .password("encodedPass")
                .role(Role.ROLE_DRIVER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtils.generateToken(any(User.class))).thenReturn("mocked.jwt.token");

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("mocked.jwt.token", resp.getToken());
        assertEquals("mario@test.com", resp.getEmail());
        assertEquals("ROLE_DRIVER", resp.getRole());
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Mario Silva")
                .email("mario@test.com")
                .password("pass123")
                .role("DRIVER")
                .build();

        when(userRepository.existsByEmailIgnoreCase("mario@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest("mario@test.com", "pass123");
        User user = User.builder()
                .id(1L)
                .fullName("Mario Silva")
                .email("mario@test.com")
                .password("encodedPass")
                .role(Role.ROLE_DRIVER)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user.getEmail(), "pass123"));
        when(userRepository.findByEmailIgnoreCase("mario@test.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(user)).thenReturn("mocked.jwt.token");
        when(vehicleRepository.findByOwner(user)).thenReturn(Collections.emptyList());

        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("mocked.jwt.token", resp.getToken());
        assertEquals("ROLE_DRIVER", resp.getRole());
    }
}
