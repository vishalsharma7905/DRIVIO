package com.drivio.dto;

import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import java.math.BigDecimal;

public class FareEstimateResponse {

    private ServiceType serviceType;
    private VehicleType vehicleType;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal baseFare;
    private BigDecimal perKmRate;
    private BigDecimal perMinuteRate;
    private BigDecimal distanceFare;
    private BigDecimal durationFare;
    private BigDecimal waitingCharges = BigDecimal.ZERO;
    private BigDecimal surchargeAmount = BigDecimal.ZERO;
    private BigDecimal tollAmount = BigDecimal.ZERO;
    private BigDecimal parkingAmount = BigDecimal.ZERO;
    private BigDecimal additionalCharges;
    private BigDecimal subtotal;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalEstimatedFare;
    private Integer pricingVersion = 1;
    private Long pricingProfileVersionId;
    private boolean minFareApplied = false;
    private String disclaimer = "The displayed fare is an estimate based on selected route and terms. Final fare may adjust slightly if journey duration or route significantly deviates.";

    public FareEstimateResponse() {}

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getPerKmRate() { return perKmRate; }
    public void setPerKmRate(BigDecimal perKmRate) { this.perKmRate = perKmRate; }

    public BigDecimal getDistanceFare() { return distanceFare; }
    public void setDistanceFare(BigDecimal distanceFare) { this.distanceFare = distanceFare; }

    public BigDecimal getDurationFare() { return durationFare; }
    public void setDurationFare(BigDecimal durationFare) { this.durationFare = durationFare; }

    public BigDecimal getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(BigDecimal additionalCharges) { this.additionalCharges = additionalCharges; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal taxPercentage) { this.taxPercentage = taxPercentage; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalEstimatedFare() { return totalEstimatedFare; }
    public void setTotalEstimatedFare(BigDecimal totalEstimatedFare) { this.totalEstimatedFare = totalEstimatedFare; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public BigDecimal getPerMinuteRate() { return perMinuteRate; }
    public void setPerMinuteRate(BigDecimal perMinuteRate) { this.perMinuteRate = perMinuteRate; }

    public BigDecimal getWaitingCharges() { return waitingCharges; }
    public void setWaitingCharges(BigDecimal waitingCharges) { this.waitingCharges = waitingCharges; }

    public BigDecimal getSurchargeAmount() { return surchargeAmount; }
    public void setSurchargeAmount(BigDecimal surchargeAmount) { this.surchargeAmount = surchargeAmount; }

    public BigDecimal getTollAmount() { return tollAmount; }
    public void setTollAmount(BigDecimal tollAmount) { this.tollAmount = tollAmount; }

    public BigDecimal getParkingAmount() { return parkingAmount; }
    public void setParkingAmount(BigDecimal parkingAmount) { this.parkingAmount = parkingAmount; }

    public Integer getPricingVersion() { return pricingVersion; }
    public void setPricingVersion(Integer pricingVersion) { this.pricingVersion = pricingVersion; }

    public Long getPricingProfileVersionId() { return pricingProfileVersionId; }
    public void setPricingProfileVersionId(Long pricingProfileVersionId) { this.pricingProfileVersionId = pricingProfileVersionId; }

    public boolean isMinFareApplied() { return minFareApplied; }
    public void setMinFareApplied(boolean minFareApplied) { this.minFareApplied = minFareApplied; }
}

