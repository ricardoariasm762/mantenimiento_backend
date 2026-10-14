package com.edu.vehiculos.repository;

import com.edu.vehiculos.domain.VehicleDiagnosisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDiagnosisLogRepository extends JpaRepository<VehicleDiagnosisLog, Long> {
    List<VehicleDiagnosisLog> findByLicensePlateOrderByCreatedAtDesc(String licensePlate);
    List<VehicleDiagnosisLog> findAllByOrderByCreatedAtDesc();
}
