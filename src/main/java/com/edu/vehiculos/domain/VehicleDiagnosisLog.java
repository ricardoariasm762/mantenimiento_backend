package com.edu.vehiculos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_diagnosis_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDiagnosisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String licensePlate;

    @Column(nullable = false)
    private Double mileageKm;

    @Column(nullable = false)
    private Double kmSinceLastService;

    @Column(nullable = false)
    private Double fuelConsumptionRatio;

    @Column(columnDefinition = "TEXT")
    private String rawSymptomText;

    @Column(length = 100)
    private String detectedSystem;

    @Column(length = 150)
    private String predictedFault;

    private Double confidencePercentage;

    @Column(columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
