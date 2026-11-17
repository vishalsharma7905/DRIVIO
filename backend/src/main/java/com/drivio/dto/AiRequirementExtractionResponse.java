package com.drivio.dto;

import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import java.util.ArrayList;
import java.util.List;

public class AiRequirementExtractionResponse {

    private ServiceType serviceType;
    private VehicleType vehicleType;
    private String experiencePreference; // EXPERIENCED, STANDARD, ANY
    private String pickupDate; // YYYY-MM-DD
    private String pickupTime; // HH:mm or MORNING, AFTERNOON, EVENING, NIGHT
    private Double estimatedDistanceKm;
    private String pickupLocationHint;
    private String destinationHint;
    private String additionalNotes;
    private List<String> missingFields = new ArrayList<>();
    private boolean fallbackUsed = false;
    private String rawExtraction;

    public AiRequirementExtractionResponse() {}

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getExperiencePreference() { return experiencePreference; }
    public void setExperiencePreference(String experiencePreference) { this.experiencePreference = experiencePreference; }

    public String getPickupDate() { return pickupDate; }
    public void setPickupDate(String pickupDate) { this.pickupDate = pickupDate; }

    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public String getPickupLocationHint() { return pickupLocationHint; }
    public void setPickupLocationHint(String pickupLocationHint) { this.pickupLocationHint = pickupLocationHint; }

    public String getDestinationHint() { return destinationHint; }
    public void setDestinationHint(String destinationHint) { this.destinationHint = destinationHint; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }

    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }

    public boolean isFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }

    public String getRawExtraction() { return rawExtraction; }
    public void setRawExtraction(String rawExtraction) { this.rawExtraction = rawExtraction; }
}

