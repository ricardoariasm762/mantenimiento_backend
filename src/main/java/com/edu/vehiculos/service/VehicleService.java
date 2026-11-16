package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.User;
import com.edu.vehiculos.domain.Vehicle;
import com.edu.vehiculos.domain.dto.VehicleDto;
import com.edu.vehiculos.repository.UserRepository;
import com.edu.vehiculos.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VehicleDto registerVehicle(VehicleDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String plate = dto.getLicensePlate().trim().toUpperCase();
        if (vehicleRepository.existsByLicensePlate(plate)) {
            throw new IllegalArgumentException("Vehicle with license plate " + plate + " is already registered");
        }

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(plate)
                .brand(dto.getBrand())
                .model(dto.getModel())
                .modelYear(dto.getModelYear())
                .baseMileageKm(dto.getBaseMileageKm() != null ? dto.getBaseMileageKm() : 0.0)
                .owner(user)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getVehiclesByUser(String userEmail) {
        return vehicleRepository.findByOwnerEmail(userEmail)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private VehicleDto mapToDto(Vehicle v) {
        return VehicleDto.builder()
                .licensePlate(v.getLicensePlate())
                .brand(v.getBrand())
                .model(v.getModel())
                .modelYear(v.getModelYear())
                .baseMileageKm(v.getBaseMileageKm())
                .ownerId(v.getOwner() != null ? v.getOwner().getId() : null)
                .ownerName(v.getOwner() != null ? v.getOwner().getFullName() : null)
                .build();
    }
}
