package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.User;
import com.edu.vehiculos.domain.WorkshopOrder;
import com.edu.vehiculos.domain.dto.CreateWorkshopOrderRequest;
import com.edu.vehiculos.domain.dto.WorkshopOrderDto;
import com.edu.vehiculos.repository.UserRepository;
import com.edu.vehiculos.repository.WorkshopOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkshopOrderService {

    private final WorkshopOrderRepository workshopOrderRepository;
    private final UserRepository userRepository;

    public WorkshopOrderService(WorkshopOrderRepository workshopOrderRepository, UserRepository userRepository) {
        this.workshopOrderRepository = workshopOrderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkshopOrderDto createOrder(CreateWorkshopOrderRequest request, String mechanicEmail) {
        User mechanic = userRepository.findByEmail(mechanicEmail)
                .orElseThrow(() -> new IllegalArgumentException("Mechanic not found"));

        String orderNumber = "WO-" + System.currentTimeMillis() % 100000;
        String priority = request.priority() != null ? request.priority() : "ALTA";

        WorkshopOrder order = new WorkshopOrder(
                orderNumber,
                request.licensePlate().trim().toUpperCase(),
                request.faultCategory(),
                priority,
                request.technicalNotes(),
                mechanic
        );

        WorkshopOrder saved = workshopOrderRepository.save(order);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkshopOrderDto> getOrdersByPlate(String plate) {
        return workshopOrderRepository.findByLicensePlateOrderByCreatedAtDesc(plate.trim().toUpperCase())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkshopOrderDto> getAllOrders() {
        return workshopOrderRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private WorkshopOrderDto mapToDto(WorkshopOrder o) {
        return new WorkshopOrderDto(
                o.getId(),
                o.getOrderNumber(),
                o.getLicensePlate(),
                o.getFaultCategory(),
                o.getPriority(),
                o.getStatus(),
                o.getTechnicalNotes(),
                o.getIssuedBy() != null ? o.getIssuedBy().getFullName() : "Authorized Mechanic",
                o.getCreatedAt()
        );
    }
}
