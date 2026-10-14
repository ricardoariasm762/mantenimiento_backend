package com.edu.vehiculos.controller;

import com.edu.vehiculos.domain.VehicleDiagnosisLog;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisRequest;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisResponse;
import com.edu.vehiculos.service.VehicleMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<VehicleDiagnosisResponse> diagnoseVehicle(@RequestBody VehicleDiagnosisRequest request) {
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
        log.info("Recibida solicitud GET /api/vehicles/history (todos los registros)");
        List<VehicleDiagnosisLog> history = maintenanceService.getHistoryByLicensePlate(null);
        return ResponseEntity.ok(history);
    }
}
