package com.edu.vehiculos.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDiagnosisRequest {
    private String licensePlate;
    private double mileageKm;
    private double kmSinceLastService;
    private double fuelConsumptionRatio; // 1.0 = normal, >1.25 = anómalo
    private String rawSymptomText;       // Síntoma descrito por voz/texto
}
