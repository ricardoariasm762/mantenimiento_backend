package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.VehicleDiagnosisLog;
import com.edu.vehiculos.domain.dto.SymptomExtractionResult;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisRequest;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisResponse;
import com.edu.vehiculos.engine.VehicleFaultInferenceEngine;
import com.edu.vehiculos.repository.VehicleDiagnosisLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleMaintenanceService {

    private final SymptomNlpService symptomNlpService;
    private final VehicleFaultInferenceEngine inferenceEngine;
    private final VehicleDiagnosisLogRepository logRepository;

    private static final String LEGAL_DISCLAIMER =
            "Herramienta orientativa de apoyo, no sustituye un diagnóstico mecánico profesional.";

    @Transactional
    public VehicleDiagnosisResponse diagnoseVehicle(VehicleDiagnosisRequest request) {
        log.info("Iniciando diagnóstico para vehículo con placa: {}", request.getLicensePlate());

        // 1. Capa NLP: Extraer subsistema y severidad desde el síntoma reportado
        SymptomExtractionResult nlpResult = symptomNlpService.extractEntitiesFromSymptom(request.getRawSymptomText());
        log.info("Resultado NLP -> Subsistema: {} (código {}), Severidad: {}",
                nlpResult.getDetectedComponent(), nlpResult.getSymptomSystemCode(), nlpResult.getSymptomSeverity());

        // 2. Capa Predictiva Numérica (ONNX Runtime): Clasificación con telemetría y NLP
        VehicleFaultInferenceEngine.PredictionResult prediction = inferenceEngine.predict(
                request.getMileageKm(),
                request.getKmSinceLastService(),
                request.getFuelConsumptionRatio(),
                nlpResult.getSymptomSystemCode(),
                nlpResult.getSymptomSeverity()
        );

        // 3. Evaluar Nivel de Riesgo y Prescribir Acción Preventiva
        String riskLevel = calculateRiskLevel(prediction.getClassIndex(), nlpResult.getSymptomSeverity(),
                request.getKmSinceLastService(), prediction.getConfidencePercentage());
        String recommendedAction = determineRecommendedAction(prediction.getClassIndex(), nlpResult.getSymptomSeverity());
        String primaryFaultDescription = formatPrimaryFault(prediction.getPredictedFault());

        // 4. Persistir Bitácora en Base de Datos PostgreSQL
        VehicleDiagnosisLog logEntity = VehicleDiagnosisLog.builder()
                .licensePlate(request.getLicensePlate() != null ? request.getLicensePlate().toUpperCase().trim() : "SIN-PLACA")
                .mileageKm(request.getMileageKm())
                .kmSinceLastService(request.getKmSinceLastService())
                .fuelConsumptionRatio(request.getFuelConsumptionRatio())
                .rawSymptomText(request.getRawSymptomText())
                .detectedSystem(nlpResult.getDetectedComponent())
                .predictedFault(primaryFaultDescription)
                .confidencePercentage(prediction.getConfidencePercentage())
                .recommendedAction(recommendedAction)
                .build();

        VehicleDiagnosisLog savedLog = logRepository.save(logEntity);
        log.info("Registro de diagnóstico guardado con ID: {}", savedLog.getId());

        // 5. Retornar DTO de Respuesta
        return VehicleDiagnosisResponse.builder()
                .licensePlate(logEntity.getLicensePlate())
                .primaryFault(primaryFaultDescription)
                .confidencePercentage(prediction.getConfidencePercentage())
                .riskLevel(riskLevel)
                .recommendedAction(recommendedAction)
                .disclaimer(LEGAL_DISCLAIMER)
                .build();
    }

    public List<VehicleDiagnosisLog> getHistoryByLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            return logRepository.findAllByOrderByCreatedAtDesc();
        }
        return logRepository.findByLicensePlateOrderByCreatedAtDesc(licensePlate.toUpperCase().trim());
    }

    private String calculateRiskLevel(int classIndex, int severity, double kmSinceService, double confidence) {
        if (classIndex == 5) { // Estado Normal
            return "BAJO";
        }
        if (severity == 3 || kmSinceService > 18000 || (confidence >= 85.0 && (classIndex == 1 || classIndex == 3))) {
            return "CRÍTICO";
        }
        return "MEDIO";
    }

    private String formatPrimaryFault(String predictedFault) {
        if ("Normal".equalsIgnoreCase(predictedFault)) {
            return "Parámetros Normales (Sin Falla Detectada)";
        }
        return "Falla en Sistema de " + predictedFault;
    }

    private String determineRecommendedAction(int classIndex, int severity) {
        return switch (classIndex) {
            case 0 -> "Realizar inspección técnica prioritaria en taller de caja y transmisión: revisar nivel y calidad de aceite de transmisión (ATF/valvulina), estado de los discos de embrague (clutch), juntas homocinéticas y soportes de motor/caja.";
            case 1 -> "Inspección urgente de seguridad en el sistema de frenado: comprobar espesor de pastillas, estado de los discos/tambores, calibración de mordazas y purga del líquido de frenos DOT antes de circular.";
            case 2 -> "Revisión preventiva del tren de rodaje: evaluar fatiga en amortiguadores, bujes de tijera, terminales de dirección, bieletas de barra estabilizadora y realizar alineación/balanceo computarizado.";
            case 3 -> "Diagnóstico por escáner OBD-II del tren motriz: verificar códigos DTC de mezcla, estado de sensores de oxígeno (lambda), inyectores, bujías y realizar prueba de compresión de cilindros.";
            case 4 -> "Chequeo del sistema electro-electrónico: prueba de carga de la batería, verificación del regulador de voltaje del alternador, inspección de terminales sulfatados y escaneo de módulos de control.";
            case 5 -> "El vehículo presenta un comportamiento operativo regular. Se aconseja continuar con el plan preventivo rutinario y programar el próximo mantenimiento según la pauta del fabricante.";
            default -> "Se recomienda una evaluación mecánica general en taller especializado para verificar los sistemas del vehículo.";
        };
    }
}
