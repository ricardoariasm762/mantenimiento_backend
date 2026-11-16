package com.edu.vehiculos.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {

    @NotBlank(message = "License plate is required")
    private String licensePlate;
    private String brand;
    private String model;
    private Integer modelYear;
    private Double baseMileageKm;
    private Long ownerId;
    private String ownerName;
}
