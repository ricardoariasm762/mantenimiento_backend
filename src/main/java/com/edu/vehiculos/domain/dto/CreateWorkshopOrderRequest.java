package com.edu.vehiculos.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkshopOrderRequest(
        @NotBlank(message = "Vehicle license plate is required")
        String licensePlate,

        @NotBlank(message = "Fault category is required")
        String faultCategory,

        String priority,

        String technicalNotes
) {}
