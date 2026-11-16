package com.edu.vehiculos.config;

import com.edu.vehiculos.domain.Role;
import com.edu.vehiculos.domain.User;
import com.edu.vehiculos.domain.Vehicle;
import com.edu.vehiculos.repository.UserRepository;
import com.edu.vehiculos.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Initialize default Driver
        if (!userRepository.existsByEmailIgnoreCase("driver@veloce.com")) {
            User driver = User.builder()
                    .fullName("Carlos Conductor")
                    .email("driver@veloce.com")
                    .password(passwordEncoder.encode("driver123"))
                    .phone("+57 300 123 4567")
                    .role(Role.ROLE_DRIVER)
                    .build();
            User savedDriver = userRepository.save(driver);
            log.info("Default Driver user created: driver@veloce.com / driver123");

            // Assign vehicle TRX-720 to the driver
            if (!vehicleRepository.existsByLicensePlateIgnoreCase("TRX-720")) {
                Vehicle vehicle = Vehicle.builder()
                        .licensePlate("TRX-720")
                        .brand("Toyota")
                        .model("Hilux Turbo-Diesel")
                        .modelYear(2022)
                        .baseMileageKm(64820.0)
                        .owner(savedDriver)
                        .build();
                vehicleRepository.save(vehicle);
                log.info("Vehicle TRX-720 assigned to driver@veloce.com");
            }
        }

        // Initialize default Mechanic
        if (!userRepository.existsByEmailIgnoreCase("mecanico@veloce.com")) {
            User mechanic = User.builder()
                    .fullName("Ing. Mateo Celli")
                    .email("mecanico@veloce.com")
                    .password(passwordEncoder.encode("mecanico123"))
                    .phone("+57 311 987 6543")
                    .role(Role.ROLE_MECHANIC)
                    .build();
            userRepository.save(mechanic);
            log.info("Default Mechanic user created: mecanico@veloce.com / mecanico123");
        }
    }
}
