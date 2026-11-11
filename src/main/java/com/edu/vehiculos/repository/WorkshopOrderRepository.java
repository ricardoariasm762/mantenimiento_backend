package com.edu.vehiculos.repository;

import com.edu.vehiculos.domain.WorkshopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopOrderRepository extends JpaRepository<WorkshopOrder, Long> {
    List<WorkshopOrder> findByLicensePlateOrderByCreatedAtDesc(String licensePlate);
    List<WorkshopOrder> findByStatusOrderByCreatedAtDesc(String status);
    Optional<WorkshopOrder> findByOrderNumber(String orderNumber);
}

