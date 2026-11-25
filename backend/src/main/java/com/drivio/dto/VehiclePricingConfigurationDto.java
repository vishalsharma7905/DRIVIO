package com.drivio.dto;

import com.drivio.enums.VehicleType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class VehiclePricingConfigurationDto {
    private Long id;

    @NotNull
    private VehicleType vehicleCategory;

    @NotNull @DecimalMin("0.0")
    private BigDecimal baseFare;

    @NotNull @DecimalMin("0.0")
    private BigDecimal perKmRate;

    @NotNull @DecimalMin("0.0")
    private BigDecimal perMinuteRate;

    @NotNull @DecimalMin("0.0")
    private BigDecimal minFare;

    @Min(0)
    private Integer waitingFreeMinutes = 3;

    @DecimalMin("0.0")
    private BigDecimal waitingPerMinuteRate = new BigDecimal("1.00");

    @DecimalMin("0.0") @DecimalMax("50.0")
    private BigDecimal nightSurchargePercent = new BigDecimal("10.00");

    @DecimalMin("10.0") @DecimalMax("30.0")
    private BigDecimal peakSurchargePercent = new BigDecimal("20.00");

    @DecimalMin("25.0") @DecimalMax("50.0")
    private BigDecimal cancellationFee = new BigDecimal("35.00");

    @DecimalMin("0.0")
    private BigDecimal taxPercentage = new BigDecimal("5.00");

    private Integer version = 1;
    private boolean active = true;

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
}
