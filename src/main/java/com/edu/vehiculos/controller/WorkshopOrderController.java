package com.edu.vehiculos.controller;

import com.edu.vehiculos.domain.dto.CreateWorkshopOrderRequest;
import com.edu.vehiculos.domain.dto.WorkshopOrderDto;
import com.edu.vehiculos.service.WorkshopOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workshop-orders")
public class WorkshopOrderController {

    private final WorkshopOrderService workshopOrderService;

    public WorkshopOrderController(WorkshopOrderService workshopOrderService) {
        this.workshopOrderService = workshopOrderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<WorkshopOrderDto> createOrder(
            @Valid @RequestBody CreateWorkshopOrderRequest request,
            Authentication authentication
    ) {
        WorkshopOrderDto order = workshopOrderService.createOrder(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/plate/{plate}")
    @PreAuthorize("hasAnyRole('DRIVER', 'MECHANIC')")
    public ResponseEntity<List<WorkshopOrderDto>> getOrdersByPlate(@PathVariable String plate) {
        return ResponseEntity.ok(workshopOrderService.getOrdersByPlate(plate));
    }

    @GetMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<List<WorkshopOrderDto>> getAllOrders() {
        return ResponseEntity.ok(workshopOrderService.getAllOrders());
    }
}

