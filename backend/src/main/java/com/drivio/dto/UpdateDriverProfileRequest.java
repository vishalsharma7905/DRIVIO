package com.drivio.dto;

import java.time.LocalDate;

public class UpdateDriverProfileRequest {
    private String fullName;
    private String phoneNumber;
    private String profilePhotoUrl;
    private String licenceNumber;
    private LocalDate licenceExpiryDate;
    private String licenceDocumentUrl;
    private Integer experienceYears;
    private String languages;
    private String vehicleCategories;
    private String servicePreferences;

    public UpdateDriverProfileRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getLicenceNumber() { return licenceNumber; }
    public void setLicenceNumber(String licenceNumber) { this.licenceNumber = licenceNumber; }

    public LocalDate getLicenceExpiryDate() { return licenceExpiryDate; }
    public void setLicenceExpiryDate(LocalDate licenceExpiryDate) { this.licenceExpiryDate = licenceExpiryDate; }

    public String getLicenceDocumentUrl() { return licenceDocumentUrl; }
    public void setLicenceDocumentUrl(String licenceDocumentUrl) { this.licenceDocumentUrl = licenceDocumentUrl; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public String getVehicleCategories() { return vehicleCategories; }
    public void setVehicleCategories(String vehicleCategories) { this.vehicleCategories = vehicleCategories; }

    public String getServicePreferences() { return servicePreferences; }
    public void setServicePreferences(String servicePreferences) { this.servicePreferences = servicePreferences; }
}

