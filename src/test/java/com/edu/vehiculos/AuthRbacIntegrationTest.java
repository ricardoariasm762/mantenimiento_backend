package com.edu.vehiculos;

import com.edu.vehiculos.domain.dto.AuthResponse;
import com.edu.vehiculos.domain.dto.LoginRequest;
import com.edu.vehiculos.domain.dto.RegisterRequest;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisRequest;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisResponse;
import com.edu.vehiculos.service.AuthService;
import com.edu.vehiculos.service.VehicleMaintenanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthRbacIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private VehicleMaintenanceService maintenanceService;

    @Test
    @DisplayName("Should register and authenticate a Driver and generate JWT token with ROLE_DRIVER")
    void testRegisterAndLoginDriver() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Juan Perez")
                .email("juan.driver@test.com")
                .password("password123")
                .phone("+573001112233")
                .role("DRIVER")
                .licensePlate("ABC-789")
                .build();

        AuthResponse regResponse = authService.register(registerReq);
        assertNotNull(regResponse);
        assertNotNull(regResponse.getToken());
        assertEquals("ROLE_DRIVER", regResponse.getRole());
        assertEquals("ABC-789", regResponse.getDefaultLicensePlate());

        LoginRequest loginReq = LoginRequest.builder()
                .email("juan.driver@test.com")
                .password("password123")
                .build();

        AuthResponse loginResp = authService.login(loginReq);
        assertNotNull(loginResp.getToken());
        assertEquals("ROLE_DRIVER", loginResp.getRole());
    }

    @Test
    @DisplayName("Should register and authenticate a Mechanic with ROLE_MECHANIC")
    void testRegisterAndLoginMechanic() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Carlos Taller")
                .email("carlos.mecanico@test.com")
                .password("mypassword")
                .role("MECHANIC")
                .build();

        AuthResponse regResponse = authService.register(registerReq);
        assertNotNull(regResponse);
        assertEquals("ROLE_MECHANIC", regResponse.getRole());
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void testInvalidLoginCredentials() {
        LoginRequest loginReq = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password("wrongpassword")
                .build();

        assertThrows(BadCredentialsException.class, () -> authService.login(loginReq));
    }

    @Test
    @DisplayName("RBAC: Driver can only query history for their assigned license plate")
    void testDriverRbacRestriction() {
        // Driver A with license plate AAA-111
        RegisterRequest driverA = RegisterRequest.builder()
                .fullName("Conductor A")
                .email("driverA@test.com")
                .password("pass123")
                .role("DRIVER")
                .licensePlate("AAA-111")
                .build();
        authService.register(driverA);

        // Driver B with license plate BBB-222
        RegisterRequest driverB = RegisterRequest.builder()
                .fullName("Conductor B")
                .email("driverB@test.com")
                .password("pass123")
                .role("DRIVER")
                .licensePlate("BBB-222")
                .build();
        authService.register(driverB);

        // Simulate Driver A security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("driverA@test.com", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DRIVER")))
        );

        // Diagnose own plate -> allowed
        VehicleDiagnosisRequest reqA = VehicleDiagnosisRequest.builder()
                .licensePlate("AAA-111")
                .mileageKm(50000.0)
                .kmSinceLastService(5000.0)
                .fuelConsumptionRatio(1.0)
                .rawSymptomText("Revisión rutinaria")
                .build();
        VehicleDiagnosisResponse resA = maintenanceService.diagnoseVehicle(reqA);
        assertNotNull(resA);

        // Query own plate history -> allowed
        assertDoesNotThrow(() -> maintenanceService.getHistoryByLicensePlate("AAA-111"));

        // Attempt to query Driver B's plate -> AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> maintenanceService.getHistoryByLicensePlate("BBB-222"));

        // Simulate Mechanic security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("mecanico@veloce.com", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_MECHANIC")))
        );

        // Mechanic can query any vehicle plate
        assertDoesNotThrow(() -> maintenanceService.getHistoryByLicensePlate("AAA-111"));
        assertDoesNotThrow(() -> maintenanceService.getHistoryByLicensePlate("BBB-222"));
    }
}
