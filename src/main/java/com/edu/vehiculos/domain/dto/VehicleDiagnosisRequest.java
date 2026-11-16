package com.edu.vehiculos.domain.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDiagnosisRequest {

    @NotBlank(message = "Vehicle license plate is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{3,10}$", message = "License plate must be between 3 and 10 alphanumeric characters")
    private String licensePlate;

    @PositiveOrZero(message = "Total mileage cannot be negative")
    private double mileageKm;

    @PositiveOrZero(message = "Mileage since last service cannot be negative")
    private double kmSinceLastService;

    @DecimalMin(value = "0.5", message = "Fuel consumption ratio must be at least 0.5")
    @DecimalMax(value = "3.0", message = "Fuel consumption ratio cannot exceed 3.0")
    private double fuelConsumptionRatio; // 1.0 = normal, >1.25 = anomalous

    @NotBlank(message = "Symptom description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String rawSymptomText; // Symptom described via voice/text
}
