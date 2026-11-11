package com.edu.vehiculos.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_orders")
public class WorkshopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 32)
    private String orderNumber;

    @Column(name = "license_plate", nullable = false, length = 16)
    private String licensePlate;

    @Column(name = "fault_category", nullable = false, length = 64)
    private String faultCategory;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority; // CRITICA, ALTA, MEDIA, BAJA

    @Column(name = "status", nullable = false, length = 20)
    private String status; // EMITIDA, EN_PROCESO, COMPLETADA, CANCELADA

    @Column(columnDefinition = "TEXT")
    private String technicalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_id", nullable = false)
    private User issuedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WorkshopOrder() {
        this.createdAt = LocalDateTime.now();
        this.status = "EMITIDA";
    }

    public WorkshopOrder(String orderNumber, String licensePlate, String faultCategory, String priority, String technicalNotes, User issuedBy) {
        this();
        this.orderNumber = orderNumber;
        this.licensePlate = licensePlate;
        this.faultCategory = faultCategory;
        this.priority = priority;
        this.technicalNotes = technicalNotes;
        this.issuedBy = issuedBy;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getFaultCategory() { return faultCategory; }
    public void setFaultCategory(String faultCategory) { this.faultCategory = faultCategory; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTechnicalNotes() { return technicalNotes; }
    public void setTechnicalNotes(String technicalNotes) { this.technicalNotes = technicalNotes; }

    public User getIssuedBy() { return issuedBy; }
    public void setIssuedBy(User issuedBy) { this.issuedBy = issuedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

