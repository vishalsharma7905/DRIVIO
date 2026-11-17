package com.drivio.dto;

import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class RecommendationRequest {

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;

    private LocalDateTime scheduledPickupTime;
    private Double maxRadiusKm = 50.0;

    public RecommendationRequest() {}

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public LocalDateTime getScheduledPickupTime() { return scheduledPickupTime; }
    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) { this.scheduledPickupTime = scheduledPickupTime; }

    public Double getMaxRadiusKm() { return maxRadiusKm; }
    public void setMaxRadiusKm(Double maxRadiusKm) { this.maxRadiusKm = maxRadiusKm; }
}

