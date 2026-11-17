package com.drivio.entity;

import com.drivio.enums.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "licence_number")
    private String licenceNumber;

    @Column(name = "licence_expiry_date")
    private LocalDate licenceExpiryDate;

    @Column(name = "licence_document_url")
    private String licenceDocumentUrl;

    @Column(name = "experience_years")
    private Integer experienceYears = 0;

    @Column(name = "languages")
    private String languages = "English,Hindi";

    @Column(name = "vehicle_categories")
    private String vehicleCategories = "HATCHBACK,SEDAN";

    @Column(name = "service_preferences")
    private String servicePreferences = "OWN_CAR_DRIVER,ONE_WAY,ROUND_TRIP";

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "rating")
    private Double rating = 5.0;

    @Column(name = "total_ratings_count")
    private Integer totalRatingsCount = 0;

    @Column(name = "total_trips")
    private Integer totalTrips = 0;

    @Column(name = "total_earnings", precision = 12, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToOne(mappedBy = "driverProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DriverAvailability availability;

    @OneToMany(mappedBy = "driverProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DriverDocument> documents = new ArrayList<>();

    public DriverProfile() {}

    public DriverProfile(User user) {
        this.user = user;
        this.verificationStatus = VerificationStatus.PENDING;
        this.rating = 5.0;
        this.totalRatingsCount = 0;
        this.totalTrips = 0;
        this.totalEarnings = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getTotalRatingsCount() { return totalRatingsCount; }
    public void setTotalRatingsCount(Integer totalRatingsCount) { this.totalRatingsCount = totalRatingsCount; }

    public Integer getTotalTrips() { return totalTrips; }
    public void setTotalTrips(Integer totalTrips) { this.totalTrips = totalTrips; }

    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }

    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }

    public LocalDateTime getLastLocationUpdate() { return lastLocationUpdate; }
    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate) { this.lastLocationUpdate = lastLocationUpdate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public DriverAvailability getAvailability() { return availability; }
    public void setAvailability(DriverAvailability availability) { this.availability = availability; }

    public List<DriverDocument> getDocuments() { return documents; }
    public void setDocuments(List<DriverDocument> documents) { this.documents = documents; }
}

