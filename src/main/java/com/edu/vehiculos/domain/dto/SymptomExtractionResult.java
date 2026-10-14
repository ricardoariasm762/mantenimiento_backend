package com.edu.vehiculos.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SymptomExtractionResult {
    private int symptomSystemCode; // 0: Transmisión, 1: Frenos, 2: Suspensión, 3: Motor, 4: Eléctrico
    private int symptomSeverity;   // 1: Leve, 2: Moderado, 3: Crítico
    private String detectedComponent;
}
