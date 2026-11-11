package com.edu.vehiculos.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre completo es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String fullName;

    @NotBlank(message = "El correo electrónico es requerido")
    @Email(message = "El formato de correo electrónico es inválido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    private String phone;

    @NotBlank(message = "El rol es requerido (DRIVER o MECHANIC)")
    private String role; // "DRIVER", "MECHANIC", "ROLE_DRIVER", "ROLE_MECHANIC"

    // Datos opcionales para asignar un vehículo inicial (útil para conductores)
    private String licensePlate;
    private String brand;
    private String model;
    private Integer modelYear;
    private Double baseMileageKm;
}

