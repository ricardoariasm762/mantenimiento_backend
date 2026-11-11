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
    @DisplayName("Debe registrar y autenticar un Conductor y generar token JWT con rol ROLE_DRIVER")
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
    @DisplayName("Debe registrar y autenticar un Mecánico con rol ROLE_MECHANIC")
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
    @DisplayName("Debe rechazar login con credenciales incorrectas")
    void testInvalidLoginCredentials() {
        LoginRequest loginReq = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password("wrongpassword")
                .build();

        assertThrows(BadCredentialsException.class, () -> authService.login(loginReq));
    }

    @Test
    @DisplayName("RBAC: Conductor solo puede consultar el historial de su placa asignada")
    void testDriverRbacRestriction() {
        // Conductor A con placa AAA-111
        RegisterRequest driverA = RegisterRequest.builder()
                .fullName("Conductor A")
                .email("driverA@test.com")
                .password("pass123")
                .role("DRIVER")
                .licensePlate("AAA-111")
                .build();
        authService.register(driverA);

        // Conductor B con placa BBB-222
        RegisterRequest driverB = RegisterRequest.builder()
                .fullName("Conductor B")
                .email("driverB@test.com")
                .password("pass123")
                .role("DRIVER")
                .licensePlate("BBB-222")
                .build();
        authService.register(driverB);

        // Simular sesión de Conductor A
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("driverA@test.com", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DRIVER")))
        );

        // Diagnosticar su propia placa -> permitido
        VehicleDiagnosisRequest reqA = VehicleDiagnosisRequest.builder()
                .licensePlate("AAA-111")
                .mileageKm(50000.0)
                .kmSinceLastService(5000.0)
                .fuelConsumptionRatio(1.0)
                .rawSymptomText("Revisión rutinaria")
                .build();
        VehicleDiagnosisResponse resA = maintenanceService.diagnoseVehicle(reqA);
        assertNotNull(resA);

        // Consultar su propia placa -> permitido
        assertDoesNotThrow(() -> maintenanceService.getHistoryByLicensePlate("AAA-111"));

        // Intentar consultar la placa de Conductor B -> AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> maintenanceService.getHistoryByLicensePlate("BBB-222"));

        // Ahora simular sesión de Mecánico
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("mecanico@veloce.com", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_MECHANIC")))
        );

        // Mecánico PUEDE consultar cualquier placa
        assertDoesNotThrow(() -> maintenanceService.getHistoryByLicensePlate("AAA-111"));
        assertDoesNotThrow(() -> maintenanceService.getHistoryByLicensePlate("BBB-222"));
    }
}

