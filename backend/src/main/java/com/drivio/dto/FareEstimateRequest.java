package com.drivio.dto;

import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class FareEstimateRequest {

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @PositiveOrZero(message = "Estimated distance must be positive or zero")
    private Double estimatedDistanceKm = 0.0;

    @PositiveOrZero(message = "Estimated duration in minutes must be positive or zero")
    private Integer estimatedDurationMinutes = 0;

    @PositiveOrZero(message = "Waiting minutes must be positive or zero")
    private Integer waitingMinutes = 0;

    private boolean nightSurchargeApplied = false;
    private boolean peakSurchargeApplied = false;
    private boolean stackPeakAndNight = false;

    @DecimalMin(value = "0.0", message = "Toll amount must be non-negative")
    private BigDecimal tollAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Parking amount must be non-negative")
    private BigDecimal parkingAmount = BigDecimal.ZERO;

    public FareEstimateRequest() {}

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public Integer getWaitingMinutes() { return waitingMinutes; }
    public void setWaitingMinutes(Integer waitingMinutes) { this.waitingMinutes = waitingMinutes; }

    public boolean isNightSurchargeApplied() { return nightSurchargeApplied; }
    public void setNightSurchargeApplied(boolean nightSurchargeApplied) { this.nightSurchargeApplied = nightSurchargeApplied; }

    public boolean isPeakSurchargeApplied() { return peakSurchargeApplied; }
    public void setPeakSurchargeApplied(boolean peakSurchargeApplied) { this.peakSurchargeApplied = peakSurchargeApplied; }

    public boolean isStackPeakAndNight() { return stackPeakAndNight; }
    public void setStackPeakAndNight(boolean stackPeakAndNight) { this.stackPeakAndNight = stackPeakAndNight; }

    public BigDecimal getTollAmount() { return tollAmount; }
    public void setTollAmount(BigDecimal tollAmount) { this.tollAmount = tollAmount; }

    public BigDecimal getParkingAmount() { return parkingAmount; }
    public void setParkingAmount(BigDecimal parkingAmount) { this.parkingAmount = parkingAmount; }
}

