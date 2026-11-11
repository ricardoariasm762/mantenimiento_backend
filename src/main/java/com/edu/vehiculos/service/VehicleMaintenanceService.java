package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.Role;
import com.edu.vehiculos.domain.User;
import com.edu.vehiculos.domain.Vehicle;
import com.edu.vehiculos.domain.VehicleDiagnosisLog;
import com.edu.vehiculos.domain.dto.SymptomExtractionResult;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisRequest;
import com.edu.vehiculos.domain.dto.VehicleDiagnosisResponse;
import com.edu.vehiculos.domain.dto.VehicleDto;
import com.edu.vehiculos.engine.VehicleFaultInferenceEngine;
import com.edu.vehiculos.repository.UserRepository;
import com.edu.vehiculos.repository.VehicleDiagnosisLogRepository;
import com.edu.vehiculos.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleMaintenanceService {

    private final SymptomNlpService symptomNlpService;
    private final VehicleFaultInferenceEngine inferenceEngine;
    private final VehicleDiagnosisLogRepository logRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    private static final String LEGAL_DISCLAIMER =
            "Herramienta orientativa de apoyo, no sustituye un diagnóstico mecánico profesional.";

    @Transactional
    public VehicleDiagnosisResponse diagnoseVehicle(VehicleDiagnosisRequest request) {
        String licensePlate = request.getLicensePlate() != null ? request.getLicensePlate().toUpperCase().trim() : "SIN-PLACA";
        log.info("Iniciando diagnóstico para vehículo con placa: {}", licensePlate);

        // Obtener usuario autenticado si existe en el contexto de seguridad
        User currentUser = getAuthenticatedUser().orElse(null);

        // Si el usuario es conductor, validar que el vehículo le pertenece o registrarlo a su nombre
        if (currentUser != null && currentUser.getRole() == Role.ROLE_DRIVER) {
            Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlateIgnoreCase(licensePlate);
            if (existingVehicle.isPresent()) {
                Vehicle v = existingVehicle.get();
                if (v.getOwner() != null && !v.getOwner().getId().equals(currentUser.getId())) {
                    log.warn("Intento de diagnóstico no autorizado: Conductor id={} intentó diagnosticar placa asignada a otro usuario", currentUser.getId());
                    throw new AccessDeniedException("Acceso denegado: El vehículo con placa " + licensePlate + " está asignado a otro conductor.");
                } else if (v.getOwner() == null) {
                    v.setOwner(currentUser);
                    vehicleRepository.save(v);
                }
            } else {
                // Registrar automáticamente el vehículo a nombre del conductor
                Vehicle newVehicle = Vehicle.builder()
                        .licensePlate(licensePlate)
                        .brand("Genérico")
                        .model("Estándar")
                        .modelYear(2022)
                        .baseMileageKm(request.getMileageKm())
                        .owner(currentUser)
                        .build();
                vehicleRepository.save(newVehicle);
                log.info("Nuevo vehículo {} registrado para el conductor {}", licensePlate, currentUser.getEmail());
            }
        }

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

        // 4. Persistir Bitácora en Base de Datos
        VehicleDiagnosisLog logEntity = VehicleDiagnosisLog.builder()
                .licensePlate(licensePlate)
                .mileageKm(request.getMileageKm())
                .kmSinceLastService(request.getKmSinceLastService())
                .fuelConsumptionRatio(request.getFuelConsumptionRatio())
                .rawSymptomText(request.getRawSymptomText())
                .detectedSystem(nlpResult.getDetectedComponent())
                .predictedFault(primaryFaultDescription)
                .confidencePercentage(prediction.getConfidencePercentage())
                .recommendedAction(recommendedAction)
                .reportedBy(currentUser)
                .build();

        VehicleDiagnosisLog savedLog = logRepository.save(logEntity);
        log.info("Registro de diagnóstico guardado con ID: {} por usuario: {}", savedLog.getId(),
                currentUser != null ? currentUser.getEmail() : "ANÓNIMO/SISTEMA");

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
            return getAllHistory();
        }

        String plate = licensePlate.toUpperCase().trim();
        User currentUser = getAuthenticatedUser().orElse(null);

        // Validación de permisos según rol
        if (currentUser != null && currentUser.getRole() == Role.ROLE_DRIVER) {
            boolean isAssigned = isVehicleAssignedToUser(plate, currentUser);
            if (!isAssigned) {
                log.warn("Acceso denegado: Conductor {} intentó consultar historial de placa no asignada: {}", currentUser.getEmail(), plate);
                throw new AccessDeniedException("Acceso denegado: No tiene permisos para consultar el historial del vehículo " + plate + ". Los conductores solo pueden consultar sus vehículos asignados.");
            }
        }

        return logRepository.findByLicensePlateOrderByCreatedAtDesc(plate);
    }

    public List<VehicleDiagnosisLog> getAllHistory() {
        User currentUser = getAuthenticatedUser().orElse(null);

        // Si es conductor, solo puede ver el historial de sus reportes
        if (currentUser != null && currentUser.getRole() == Role.ROLE_DRIVER) {
            return logRepository.findByReportedByOrderByCreatedAtDesc(currentUser);
        }

        // Si es mecánico o llamada interna, puede ver todo el historial global
        return logRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Vehicle> getVehiclesForCurrentUser() {
        User currentUser = getAuthenticatedUser()
                .orElseThrow(() -> new AccessDeniedException("Debe estar autenticado para consultar vehículos"));

        if (currentUser.getRole() == Role.ROLE_MECHANIC) {
            return vehicleRepository.findAll();
        }
        return vehicleRepository.findByOwner(currentUser);
    }

    @Transactional
    public Vehicle registerVehicle(VehicleDto dto) {
        User currentUser = getAuthenticatedUser()
                .orElseThrow(() -> new AccessDeniedException("Debe estar autenticado para registrar un vehículo"));

        String plate = dto.getLicensePlate().toUpperCase().trim();
        Optional<Vehicle> existing = vehicleRepository.findByLicensePlateIgnoreCase(plate);
        if (existing.isPresent()) {
            Vehicle v = existing.get();
            if (v.getOwner() != null && !v.getOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("El vehículo con placa " + plate + " ya está asignado a otro usuario.");
            }
            v.setOwner(currentUser);
            if (dto.getBrand() != null) v.setBrand(dto.getBrand());
            if (dto.getModel() != null) v.setModel(dto.getModel());
            if (dto.getModelYear() != null) v.setModelYear(dto.getModelYear());
            if (dto.getBaseMileageKm() != null) v.setBaseMileageKm(dto.getBaseMileageKm());
            return vehicleRepository.save(v);
        }

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(plate)
                .brand(dto.getBrand() != null ? dto.getBrand() : "Genérico")
                .model(dto.getModel() != null ? dto.getModel() : "Estándar")
                .modelYear(dto.getModelYear() != null ? dto.getModelYear() : 2022)
                .baseMileageKm(dto.getBaseMileageKm() != null ? dto.getBaseMileageKm() : 50000.0)
                .owner(currentUser)
                .build();

        return vehicleRepository.save(vehicle);
    }

    private boolean isVehicleAssignedToUser(String plate, User user) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByLicensePlateIgnoreCase(plate);
        if (vehicleOpt.isPresent() && vehicleOpt.get().getOwner() != null) {
            return vehicleOpt.get().getOwner().getId().equals(user.getId());
        }
        // Fallback: verificar si el conductor ha emitido diagnósticos previamente para esta placa
        List<VehicleDiagnosisLog> logs = logRepository.findByLicensePlateOrderByCreatedAtDesc(plate);
        return logs.stream().anyMatch(l -> l.getReportedBy() != null && l.getReportedBy().getId().equals(user.getId()));
    }

    private Optional<User> getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(auth.getName());
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
