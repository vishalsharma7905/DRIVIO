package com.drivio.entity;

import com.drivio.enums.VehicleType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_pricing_configurations")
public class VehiclePricingConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false, unique = true)
    private VehicleType vehicleCategory;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "per_km_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal perKmRate;

    @Column(name = "per_minute_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal perMinuteRate;

    @Column(name = "min_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal minFare;

    @Column(name = "waiting_free_minutes", nullable = false)
    private Integer waitingFreeMinutes = 3;

    @Column(name = "waiting_per_minute_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal waitingPerMinuteRate = new BigDecimal("1.00");

    @Column(name = "night_surcharge_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal nightSurchargePercent = new BigDecimal("10.00");

    @Column(name = "peak_surcharge_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal peakSurchargePercent = new BigDecimal("20.00");

    @Column(name = "cancellation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal cancellationFee = new BigDecimal("35.00");

    @Column(name = "tax_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercentage = new BigDecimal("5.00");

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

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

    public VehicleType getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(VehicleType vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getPerKmRate() { return perKmRate; }
    public void setPerKmRate(BigDecimal perKmRate) { this.perKmRate = perKmRate; }

    public BigDecimal getPerMinuteRate() { return perMinuteRate; }
    public void setPerMinuteRate(BigDecimal perMinuteRate) { this.perMinuteRate = perMinuteRate; }

    public BigDecimal getMinFare() { return minFare; }
    public void setMinFare(BigDecimal minFare) { this.minFare = minFare; }

    public Integer getWaitingFreeMinutes() { return waitingFreeMinutes; }
    public void setWaitingFreeMinutes(Integer waitingFreeMinutes) { this.waitingFreeMinutes = waitingFreeMinutes; }

    public BigDecimal getWaitingPerMinuteRate() { return waitingPerMinuteRate; }
    public void setWaitingPerMinuteRate(BigDecimal waitingPerMinuteRate) { this.waitingPerMinuteRate = waitingPerMinuteRate; }

    public BigDecimal getNightSurchargePercent() { return nightSurchargePercent; }
    public void setNightSurchargePercent(BigDecimal nightSurchargePercent) { this.nightSurchargePercent = nightSurchargePercent; }

    public BigDecimal getPeakSurchargePercent() { return peakSurchargePercent; }
    public void setPeakSurchargePercent(BigDecimal peakSurchargePercent) { this.peakSurchargePercent = peakSurchargePercent; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal taxPercentage) { this.taxPercentage = taxPercentage; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
