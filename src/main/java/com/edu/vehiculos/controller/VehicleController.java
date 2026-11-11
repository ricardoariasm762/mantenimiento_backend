package com.edu.vehiculos.controller;

import com.edu.vehiculos.domain.dto.VehicleDto;
import com.edu.vehiculos.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles/fleet")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'MECHANIC')")
    public ResponseEntity<VehicleDto> registerVehicle(
            @Valid @RequestBody VehicleDto dto,
            Authentication authentication
    ) {
        VehicleDto registered = vehicleService.registerVehicle(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @GetMapping("/my-vehicles")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<VehicleDto>> getMyVehicles(Authentication authentication) {
        return ResponseEntity.ok(vehicleService.getVehiclesByUser(authentication.getName()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<List<VehicleDto>> getAllFleet() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }
}

