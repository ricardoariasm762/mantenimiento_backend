package com.edu.vehiculos.controller;

import com.edu.vehiculos.domain.Vehicle;
import com.edu.vehiculos.domain.VehicleDiagnosisLog;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisRequest;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisResponse;
import com.edu.vehiculos.domain.dto.VehicleDto;
import com.edu.vehiculos.service.VehicleMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@RequiredArgsConstructor
@Slf4j
public class VehicleDiagnosisController {

    private final VehicleMaintenanceService maintenanceService;

    @PostMapping("/diagnose")
    public ResponseEntity<VehicleDiagnosisResponse> diagnoseVehicle(@Valid @RequestBody VehicleDiagnosisRequest request) {
        log.info("Recibida solicitud POST /api/vehicles/diagnose para placa: {}", request.getLicensePlate());
        VehicleDiagnosisResponse response = maintenanceService.diagnoseVehicle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{licensePlate}")
    public ResponseEntity<List<VehicleDiagnosisLog>> getHistory(@PathVariable String licensePlate) {
        log.info("Recibida solicitud GET /api/vehicles/history/{}", licensePlate);
        List<VehicleDiagnosisLog> history = maintenanceService.getHistoryByLicensePlate(licensePlate);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VehicleDiagnosisLog>> getAllHistory() {
        log.info("Recibida solicitud GET /api/vehicles/history");
        List<VehicleDiagnosisLog> history = maintenanceService.getAllHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/my-vehicles")
    public ResponseEntity<List<Vehicle>> getMyVehicles() {
        log.info("Recibida solicitud GET /api/vehicles/my-vehicles");
        List<Vehicle> vehicles = maintenanceService.getVehiclesForCurrentUser();
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping("/register-vehicle")
    public ResponseEntity<Vehicle> registerVehicle(@Valid @RequestBody VehicleDto dto) {
        log.info("Recibida solicitud POST /api/vehicles/register-vehicle para placa: {}", dto.getLicensePlate());
        Vehicle vehicle = maintenanceService.registerVehicle(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
    }

    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, Object>> checkHealth() {
        log.info("Recibida solicitud GET /api/vehicles/health");
        return ResponseEntity.ok(java.util.Map.of(
                "status", "UP",
                "service", "Sistema Predictivo de Mantenimiento Vehicular",
                "inferenceEngine", "ONNX Runtime 1.20",
                "authSecurity", "Spring Security 6 + JWT RBAC (ROLE_DRIVER / ROLE_MECHANIC)",
                "timestamp", java.time.LocalDateTime.now()
        ));
    }
}
