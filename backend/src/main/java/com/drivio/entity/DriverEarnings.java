package com.drivio.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_earnings")
public class DriverEarnings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_profile_id", nullable = false)
    private Long driverProfileId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status = "ACCRUED";

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "accrued_at", nullable = false, updatable = false)
    private LocalDateTime accruedAt = LocalDateTime.now();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public DriverEarnings() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDriverProfileId() { return driverProfileId; }
    public void setDriverProfileId(Long driverProfileId) { this.driverProfileId = driverProfileId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

    public LocalDateTime getAccruedAt() { return accruedAt; }
    public void setAccruedAt(LocalDateTime accruedAt) { this.accruedAt = accruedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}