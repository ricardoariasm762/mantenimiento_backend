package com.edu.vehiculos;

import com.edu.vehiculos.domain.dto.VehicleDiagnosisRequest;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisResponse;
import com.edu.vehiculos.service.VehicleMaintenanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VehicleDiagnosisIntegrationTest {

    @Autowired
    private VehicleMaintenanceService maintenanceService;

    @Test
    @DisplayName("Debe clasificar falla de transmisión a partir de síntomas y telemetría")
    void testDiagnoseTransmissionFault() {
        VehicleDiagnosisRequest request = VehicleDiagnosisRequest.builder()
                .licensePlate("ABC-123")
                .mileageKm(120000.0)
                .kmSinceLastService(14000.0)
                .fuelConsumptionRatio(1.15)
                .rawSymptomText("El carro empezó a vibrar en la palanca de cambios cuando acelero")
                .build();

        VehicleDiagnosisResponse response = maintenanceService.diagnoseVehicle(request);

        assertNotNull(response);
        assertEquals("ABC-123", response.getLicensePlate());
        assertNotNull(response.getPrimaryFault());
        assertTrue(response.getConfidencePercentage() > 0.0);
        assertNotNull(response.getRiskLevel());
        assertNotNull(response.getRecommendedAction());
        assertEquals("Herramienta orientativa de apoyo, no sustituye un diagnóstico mecánico profesional.", response.getDisclaimer());
    }

    @Test
    @DisplayName("Debe clasificar falla de frenos cuando se reporta chirrido metálico")
    void testDiagnoseBrakeFault() {
        VehicleDiagnosisRequest request = VehicleDiagnosisRequest.builder()
                .licensePlate("XYZ-999")
                .mileageKm(65000.0)
                .kmSinceLastService(13000.0)
                .fuelConsumptionRatio(1.02)
                .rawSymptomText("Escucho un chirrido agudo en los frenos al pisar el pedal")
                .build();

        VehicleDiagnosisResponse response = maintenanceService.diagnoseVehicle(request);

        assertNotNull(response);
        assertEquals("XYZ-999", response.getLicensePlate());
        assertTrue(response.getPrimaryFault().toLowerCase().contains("freno"));
        assertNotNull(response.getRecommendedAction());
    }
}
