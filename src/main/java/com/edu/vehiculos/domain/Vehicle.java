package com.edu.vehiculos.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @Column(nullable = false, unique = true, length = 30)
    private String licensePlate;

    @Column(length = 60)
    private String brand;

    @Column(length = 60)
    private String model;

    private Integer modelYear;

    private Double baseMileageKm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User owner;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (licensePlate != null) {
            licensePlate = licensePlate.toUpperCase().trim();
        }
    }
}

