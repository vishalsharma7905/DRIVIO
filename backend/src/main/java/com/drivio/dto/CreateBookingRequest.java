package com.drivio.dto;

import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateBookingRequest {

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    @NotNull(message = "Destination latitude is required")
    private Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    private Double destinationLongitude;

    @NotNull(message = "Scheduled pickup time is required")
    private LocalDateTime scheduledPickupTime;

    private Double estimatedDistanceKm = 0.0;
    private Integer estimatedDurationMinutes = 0;
    private String specialInstructions;
    private Long requestedDriverId;
    private String serviceSubtype;
    private Long vehicleId;
    private Integer luggageCount = 0;
    private Integer waitingMinutes = 0;
    private boolean nightSurchargeApplied = false;
    private boolean peakSurchargeApplied = false;
    private boolean stackPeakAndNight = false;
    private BigDecimal tollAmount = BigDecimal.ZERO;
    private BigDecimal parkingAmount = BigDecimal.ZERO;

    public CreateBookingRequest() {}

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }

    public Double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }

    public Double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }

    public LocalDateTime getScheduledPickupTime() { return scheduledPickupTime; }
    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) { this.scheduledPickupTime = scheduledPickupTime; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public Long getRequestedDriverId() { return requestedDriverId; }
    public void setRequestedDriverId(Long requestedDriverId) { this.requestedDriverId = requestedDriverId; }

    public String getServiceSubtype() { return serviceSubtype; }
    public void setServiceSubtype(String serviceSubtype) { this.serviceSubtype = serviceSubtype; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Integer getLuggageCount() { return luggageCount; }
    public void setLuggageCount(Integer luggageCount) { this.luggageCount = luggageCount; }

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

