package com.edu.vehiculos.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must have at least 6 characters")
    private String password;

    private String phone;

    @NotBlank(message = "Role is required (DRIVER or MECHANIC)")
    private String role; // "DRIVER", "MECHANIC", "ROLE_DRIVER", "ROLE_MECHANIC"

    // Optional fields to assign initial vehicle (useful for drivers)
    private String licensePlate;
    private String brand;
    private String model;
    private Integer modelYear;
    private Double baseMileageKm;
}
