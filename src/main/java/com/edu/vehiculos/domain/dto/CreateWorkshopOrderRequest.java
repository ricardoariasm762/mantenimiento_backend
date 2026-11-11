package com.edu.vehiculos.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkshopOrderRequest(
        @NotBlank(message = "La placa del vehículo es obligatoria")
        String licensePlate,

        @NotBlank(message = "La categoría de falla es obligatoria")
        String faultCategory,

        String priority,

        String technicalNotes
) {}

