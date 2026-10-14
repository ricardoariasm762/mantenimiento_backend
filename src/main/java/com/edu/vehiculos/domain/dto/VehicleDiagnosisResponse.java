package com.edu.vehiculos.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDiagnosisResponse {
    private String licensePlate;
    private String primaryFault;           // ej. "Falla en Sistema de Transmisión"
    private double confidencePercentage;    // ej. 68.5%
    private String riskLevel;               // "BAJO", "MEDIO", "CRÍTICO"
    private String recommendedAction;       // "Realizar revisión técnica preventiva en taller especializado..."
    private String disclaimer;              // "Herramienta orientativa de apoyo, no sustituye un diagnóstico mecánico profesional."
}
