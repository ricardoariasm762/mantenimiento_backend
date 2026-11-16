package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.Role;
import com.edu.vehiculos.domain.User;
import com.edu.vehiculos.domain.Vehicle;
import com.edu.vehiculos.domain.dto.AuthResponse;
import com.edu.vehiculos.domain.dto.LoginRequest;
import com.edu.vehiculos.domain.dto.RegisterRequest;
import com.edu.vehiculos.repository.UserRepository;
import com.edu.vehiculos.repository.VehicleRepository;
import com.edu.vehiculos.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("The email address " + normalizedEmail + " is already registered.");
        }

        Role assignedRole = parseRole(request.getRole());

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(assignedRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: id={}, email={}, role={}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        String defaultPlate = null;
        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            String plate = request.getLicensePlate().toUpperCase().trim();
            Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate)
                    .orElse(Vehicle.builder()
                            .licensePlate(plate)
                            .brand(request.getBrand() != null ? request.getBrand().trim() : "Generic")
                            .model(request.getModel() != null ? request.getModel().trim() : "Standard")
                            .modelYear(request.getModelYear() != null ? request.getModelYear() : 2022)
                            .baseMileageKm(request.getBaseMileageKm() != null ? request.getBaseMileageKm() : 50000.0)
                            .owner(savedUser)
                            .build());

            vehicle.setOwner(savedUser);
            vehicleRepository.save(vehicle);
            defaultPlate = plate;
            log.info("Vehicle assigned upon user registration: plate={}", plate);
        }

        String token = jwtUtils.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .phone(savedUser.getPhone())
                .defaultLicensePlate(defaultPlate)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Authentication failed for email {}: {}", normalizedEmail, ex.getMessage());
            throw new BadCredentialsException("Invalid credentials. Please verify your email and password.");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String token = jwtUtils.generateToken(user);

        // Check default vehicle plate for user if present
        List<Vehicle> userVehicles = vehicleRepository.findByOwner(user);
        String defaultPlate = userVehicles.isEmpty() ? null : userVehicles.get(0).getLicensePlate();

        log.info("Successful login for user: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .defaultLicensePlate(defaultPlate)
                .build();
    }

    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Vehicle> userVehicles = vehicleRepository.findByOwner(user);
        String defaultPlate = userVehicles.isEmpty() ? null : userVehicles.get(0).getLicensePlate();

        return AuthResponse.builder()
                .token(null) // No need to regenerate token
                .type("Bearer")
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .defaultLicensePlate(defaultPlate)
                .build();
    }

    private Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return Role.ROLE_DRIVER;
        }
        String upper = roleStr.toUpperCase().trim();
        if (upper.contains("MECHANIC") || upper.equals("MECANICO")) {
            return Role.ROLE_MECHANIC;
        }
        return Role.ROLE_DRIVER;
    }
}
