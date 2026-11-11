package com.edu.vehiculos.domain.dto;

import java.time.LocalDateTime;

public record WorkshopOrderDto(
        Long id,
        String orderNumber,
        String licensePlate,
        String faultCategory,
        String priority,
        String status,
        String technicalNotes,
        String mechanicName,
        LocalDateTime createdAt
) {}

