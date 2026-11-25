package com.drivio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_quotations")
public class BookingQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonIgnore
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    @JsonIgnore
    private DriverProfile driver;

    @Column(name = "proposed_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedFare;

    @Column(name = "customer_counter_fare", precision = 10, scale = 2)
    private BigDecimal customerCounterFare;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, COUNTERED, ACCEPTED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public DriverProfile getDriver() { return driver; }
    public void setDriver(DriverProfile driver) { this.driver = driver; }

    public BigDecimal getProposedFare() { return proposedFare; }
    public void setProposedFare(BigDecimal proposedFare) { this.proposedFare = proposedFare; }

    public BigDecimal getCustomerCounterFare() { return customerCounterFare; }
    public void setCustomerCounterFare(BigDecimal customerCounterFare) { this.customerCounterFare = customerCounterFare; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
