package com.drivio.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_settlements")
public class DriverSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_profile_id", nullable = false)
    private Long driverProfileId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, COMPLETED, FAILED

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "initiated_at", nullable = false, updatable = false)
    private LocalDateTime initiatedAt = LocalDateTime.now();

    public DriverSettlement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDriverProfileId() { return driverProfileId; }
    public void setDriverProfileId(Long driverProfileId) { this.driverProfileId = driverProfileId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(LocalDateTime initiatedAt) { this.initiatedAt = initiatedAt; }
}
