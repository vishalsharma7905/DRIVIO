package com.drivio.dto;

import java.util.List;

public class DriverRecommendationDto {

    private Long driverId;
    private String driverName;
    private String phoneNumber;
    private String profilePhotoUrl;
    private Double rating;
    private Integer totalTrips;
    private Integer experienceYears;
    private String languages;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double distanceKm;
    private Double matchScore; // 0 to 100
    private List<String> matchReasons;
    private String aiEndorsement;
    private ScoreBreakdown breakdown;

    public static class ScoreBreakdown {
        private double availabilityScore;
        private double proximityScore;
        private double ratingScore;
        private double experienceScore;
        private double vehicleCompatibilityScore;
        private double serviceCompatibilityScore;

        public ScoreBreakdown() {}

        public ScoreBreakdown(double availabilityScore, double proximityScore, double ratingScore, double experienceScore, double vehicleCompatibilityScore, double serviceCompatibilityScore) {
            this.availabilityScore = availabilityScore;
            this.proximityScore = proximityScore;
            this.ratingScore = ratingScore;
            this.experienceScore = experienceScore;
            this.vehicleCompatibilityScore = vehicleCompatibilityScore;
            this.serviceCompatibilityScore = serviceCompatibilityScore;
        }

        public double getAvailabilityScore() { return availabilityScore; }
        public void setAvailabilityScore(double availabilityScore) { this.availabilityScore = availabilityScore; }

        public double getProximityScore() { return proximityScore; }
        public void setProximityScore(double proximityScore) { this.proximityScore = proximityScore; }

        public double getRatingScore() { return ratingScore; }
        public void setRatingScore(double ratingScore) { this.ratingScore = ratingScore; }

        public double getExperienceScore() { return experienceScore; }
        public void setExperienceScore(double experienceScore) { this.experienceScore = experienceScore; }

        public double getVehicleCompatibilityScore() { return vehicleCompatibilityScore; }
        public void setVehicleCompatibilityScore(double vehicleCompatibilityScore) { this.vehicleCompatibilityScore = vehicleCompatibilityScore; }

        public double getServiceCompatibilityScore() { return serviceCompatibilityScore; }
        public void setServiceCompatibilityScore(double serviceCompatibilityScore) { this.serviceCompatibilityScore = serviceCompatibilityScore; }
    }

    public DriverRecommendationDto() {}

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getTotalTrips() { return totalTrips; }
    public void setTotalTrips(Integer totalTrips) { this.totalTrips = totalTrips; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }

    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public List<String> getMatchReasons() { return matchReasons; }
    public void setMatchReasons(List<String> matchReasons) { this.matchReasons = matchReasons; }

    public String getAiEndorsement() { return aiEndorsement; }
    public void setAiEndorsement(String aiEndorsement) { this.aiEndorsement = aiEndorsement; }

    public ScoreBreakdown getBreakdown() { return breakdown; }
    public void setBreakdown(ScoreBreakdown breakdown) { this.breakdown = breakdown; }
}

