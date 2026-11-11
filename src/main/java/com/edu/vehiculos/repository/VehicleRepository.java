package com.edu.vehiculos.repository;

import com.edu.vehiculos.domain.User;
import com.edu.vehiculos.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);
    List<Vehicle> findByOwner(User owner);
    List<Vehicle> findByOwnerEmailIgnoreCase(String email);
    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    default boolean existsByLicensePlate(String licensePlate) {
        return existsByLicensePlateIgnoreCase(licensePlate);
    }

    default List<Vehicle> findByOwnerEmail(String email) {
        return findByOwnerEmailIgnoreCase(email);
    }
}

