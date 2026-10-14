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

    @NotBlank(message = "La placa del vehículo es requerida")
    @Pattern(regexp = "^[A-Za-z0-9\\-]{3,10}$", message = "La placa debe tener entre 3 y 10 caracteres alfanuméricos")
    private String licensePlate;

    @PositiveOrZero(message = "El kilometraje total no puede ser negativo")
    private double mileageKm;

    @PositiveOrZero(message = "El kilometraje desde el último servicio no puede ser negativo")
    private double kmSinceLastService;

    @DecimalMin(value = "0.5", message = "El ratio de consumo debe ser mayor o igual a 0.5")
    @DecimalMax(value = "3.0", message = "El ratio de consumo no puede superar 3.0")
    private double fuelConsumptionRatio; // 1.0 = normal, >1.25 = anómalo

    @NotBlank(message = "La descripción del síntoma o motivo de consulta es requerida")
    @Size(max = 1000, message = "La descripción no puede exceder los 1000 caracteres")
    private String rawSymptomText;       // Síntoma descrito por voz/texto
}
