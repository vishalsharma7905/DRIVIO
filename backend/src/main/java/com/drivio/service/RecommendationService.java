package com.drivio.service;

import com.drivio.dto.DriverRecommendationDto;
import com.drivio.dto.RecommendationRequest;
import com.drivio.entity.DriverProfile;
import com.drivio.repository.BookingRepository;
import com.drivio.repository.DriverProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final DriverProfileRepository driverProfileRepository;
    private final AiService aiService;
    private final BookingRepository bookingRepository;

    // Modular weights as required:
    private static final double WEIGHT_AVAILABILITY = 0.25;
    private static final double WEIGHT_PROXIMITY = 0.25;
    private static final double WEIGHT_RATING = 0.20;
    private static final double WEIGHT_EXPERIENCE = 0.10;
    private static final double WEIGHT_VEHICLE_COMPATIBILITY = 0.10;
    private static final double WEIGHT_SERVICE_COMPATIBILITY = 0.10;

    public RecommendationService(DriverProfileRepository driverProfileRepository, AiService aiService) {
        this(driverProfileRepository, aiService, null);
    }

    @Autowired
    public RecommendationService(
            DriverProfileRepository driverProfileRepository,
            AiService aiService,
            BookingRepository bookingRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.aiService = aiService;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<DriverRecommendationDto> recommendDrivers(RecommendationRequest request) {
        // Step 1: Pre-filter - only verified drivers who have enabled availability
        List<DriverProfile> verifiedDrivers = driverProfileRepository.findAvailableAndVerifiedDrivers();

        List<DriverRecommendationDto> recommendations = new ArrayList<>();

        for (DriverProfile driver : verifiedDrivers) {
            if (bookingRepository != null && bookingRepository.hasActiveOngoingTrip(driver.getId())) {
                continue;
            }
            // Check driver location coordinates
            Double driverLat = driver.getCurrentLatitude();
            Double driverLng = driver.getCurrentLongitude();
            if (driverLat == null || driverLng == null) {
                // If location not yet broadcast, default to pickup location offset for simulation/testing
                driverLat = request.getPickupLatitude();
                driverLng = request.getPickupLongitude();
            }

            double distanceKm = calculateHaversineDistance(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    driverLat, driverLng
            );

            // Filter out drivers exceeding max radius if set
            if (request.getMaxRadiusKm() != null && distanceKm > request.getMaxRadiusKm()) {
                continue;
            }

            // Step 2: Scoring components (each 0 - 100)
            double availabilityScore = (driver.getAvailability() != null && driver.getAvailability().isAvailable()) ? 100.0 : 0.0;

            // Proximity: 100 at 0km, decays smoothly, 0 at 25km+
            double proximityScore = Math.max(0.0, 100.0 - (distanceKm * 4.0));

            // Rating: 5.0 -> 100
            double rating = driver.getRating() != null ? driver.getRating() : 5.0;
            double ratingScore = Math.min(100.0, (rating / 5.0) * 100.0);

            // Experience: 10+ years = 100
            int experience = driver.getExperienceYears() != null ? driver.getExperienceYears() : 0;
            double experienceScore = Math.min(100.0, (experience / 10.0) * 100.0);

            // Vehicle compatibility (exact token match so SEDAN != PREMIUM_SEDAN)
            boolean vehicleMatch = hasToken(driver.getVehicleCategories(), request.getVehicleType().name());
            double vehicleScore = vehicleMatch ? 100.0 : 20.0;

            // Service compatibility (exact token match)
            boolean serviceMatch = hasToken(driver.getServicePreferences(), request.getServiceType().name());
            double serviceScore = serviceMatch ? 100.0 : 30.0;

            // Step 3: Compute weighted score
            double totalScore = (availabilityScore * WEIGHT_AVAILABILITY) +
                                (proximityScore * WEIGHT_PROXIMITY) +
                                (ratingScore * WEIGHT_RATING) +
                                (experienceScore * WEIGHT_EXPERIENCE) +
                                (vehicleScore * WEIGHT_VEHICLE_COMPATIBILITY) +
                                (serviceScore * WEIGHT_SERVICE_COMPATIBILITY);

            totalScore = BigDecimal.valueOf(totalScore).setScale(1, RoundingMode.HALF_UP).doubleValue();

            // Step 4: Generate human-readable match explanations
            List<String> matchReasons = new ArrayList<>();
            if (distanceKm < 3.0) {
                matchReasons.add("Very close to pickup (" + String.format("%.1f", distanceKm) + " km away)");
            } else if (distanceKm < 8.0) {
                matchReasons.add("Within nearby service area (" + String.format("%.1f", distanceKm) + " km away)");
            }

            if (rating >= 4.8) {
                matchReasons.add("Top-rated driver with " + String.format("%.2f", rating) + "★ (" + driver.getTotalRatingsCount() + " ratings)");
            }

            if (experience >= 5) {
                matchReasons.add("Experienced driver with " + experience + "+ years of professional experience");
            }

            if (vehicleMatch) {
                matchReasons.add("Certified for " + request.getVehicleType().name() + " vehicles");
            }

            if (serviceMatch) {
                matchReasons.add("Prefers " + request.getServiceType().name().replace('_', ' ') + " journeys");
            }

            DriverRecommendationDto dto = new DriverRecommendationDto();
            dto.setDriverId(driver.getId());
            dto.setDriverName(driver.getUser().getFullName());
            dto.setPhoneNumber(driver.getUser().getPhoneNumber());
            dto.setProfilePhotoUrl(driver.getProfilePhotoUrl());
            dto.setRating(rating);
            dto.setTotalTrips(driver.getTotalTrips());
            dto.setExperienceYears(experience);
            dto.setLanguages(driver.getLanguages());
            dto.setCurrentLatitude(driverLat);
            dto.setCurrentLongitude(driverLng);
            dto.setDistanceKm(BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP).doubleValue());
            dto.setMatchScore(totalScore);
            dto.setMatchReasons(matchReasons);

            DriverRecommendationDto.ScoreBreakdown breakdown = new DriverRecommendationDto.ScoreBreakdown(
                    availabilityScore, proximityScore, ratingScore, experienceScore, vehicleScore, serviceScore
            );
            dto.setBreakdown(breakdown);

            recommendations.add(dto);
        }

        // Rank deterministically by matchScore descending, then distance ascending, then driverId ascending
        List<DriverRecommendationDto> sorted = recommendations.stream()
                .sorted(Comparator.comparing(DriverRecommendationDto::getMatchScore).reversed()
                        .thenComparing(DriverRecommendationDto::getDistanceKm)
                        .thenComparing(DriverRecommendationDto::getDriverId))
                .collect(Collectors.toList());

        // Enrich the top-ranked driver with Ollama AI match endorsement
        if (!sorted.isEmpty()) {
            DriverRecommendationDto top = sorted.get(0);
            String endorsement = aiService.generateRecommendationEndorsement(
                    top.getDriverName(),
                    top.getRating(),
                    top.getExperienceYears(),
                    top.getDistanceKm(),
                    top.getMatchScore(),
                    request.getVehicleType().name(),
                    request.getServiceType().name().replace('_', ' ')
            );
            top.setAiEndorsement(endorsement);
        }

        return sorted;
    }

    /**
     * Calculates the great-circle distance between two points on the Earth's surface using the Haversine formula.
     * @return Distance in kilometers
     */
    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private boolean hasToken(String csv, String target) {
        if (csv == null || target == null) return false;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .anyMatch(token -> token.equalsIgnoreCase(target));
    }
}

