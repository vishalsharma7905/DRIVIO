package com.drivio;

import com.drivio.dto.DriverRecommendationDto;
import com.drivio.dto.RecommendationRequest;
import com.drivio.entity.DriverAvailability;
import com.drivio.entity.DriverProfile;
import com.drivio.entity.User;
import com.drivio.enums.Role;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.enums.VerificationStatus;
import com.drivio.repository.DriverProfileRepository;
import com.drivio.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private DriverProfile driver1;
    private DriverProfile driver2;

    @BeforeEach
    void setUp() {
        User u1 = new User("driver1@drivio.com", "pass", "Rajesh Kumar", "+9198111", Role.DRIVER);
        driver1 = new DriverProfile(u1);
        driver1.setId(1L);
        driver1.setVerificationStatus(VerificationStatus.VERIFIED);
        driver1.setRating(4.9);
        driver1.setExperienceYears(8);
        driver1.setVehicleCategories("SEDAN,SUV");
        driver1.setServicePreferences("OWN_CAR_DRIVER,ONE_WAY");
        driver1.setCurrentLatitude(28.6280);
        driver1.setCurrentLongitude(77.2180);
        DriverAvailability da1 = new DriverAvailability(driver1, true);
        driver1.setAvailability(da1);

        User u2 = new User("driver2@drivio.com", "pass", "Amit Singh", "+9198222", Role.DRIVER);
        driver2 = new DriverProfile(u2);
        driver2.setId(2L);
        driver2.setVerificationStatus(VerificationStatus.VERIFIED);
        driver2.setRating(4.2);
        driver2.setExperienceYears(2);
        driver2.setVehicleCategories("HATCHBACK");
        driver2.setServicePreferences("HOURLY");
        driver2.setCurrentLatitude(28.7500); // Further away
        driver2.setCurrentLongitude(77.3000);
        DriverAvailability da2 = new DriverAvailability(driver2, true);
        driver2.setAvailability(da2);
    }

    @Test
    void testHaversineDistance() {
        // Distance between Connaught Place (28.6280, 77.2180) and India Gate (28.6129, 77.2295) is approx 2.0 km
        double distance = recommendationService.calculateHaversineDistance(28.6280, 77.2180, 28.6129, 77.2295);
        assertTrue(distance > 1.5 && distance < 2.5, "Distance should be approx 2km, got: " + distance);
    }

    @Test
    void testRecommendDrivers_RankingAndScores() {
        when(driverProfileRepository.findAvailableAndVerifiedDrivers())
                .thenReturn(Arrays.asList(driver1, driver2));

        RecommendationRequest request = new RecommendationRequest();
        request.setServiceType(ServiceType.OWN_CAR_DRIVER);
        request.setVehicleType(VehicleType.SUV);
        request.setPickupLatitude(28.6280); // Exact match with driver 1
        request.setPickupLongitude(77.2180);

        List<DriverRecommendationDto> result = recommendationService.recommendDrivers(request);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Driver 1 has 0km distance, 4.9 rating, 8 yrs experience, matches SUV and OWN_CAR_DRIVER
        DriverRecommendationDto topDriver = result.get(0);
        assertEquals(1L, topDriver.getDriverId());
        assertEquals("Rajesh Kumar", topDriver.getDriverName());
        assertTrue(topDriver.getMatchScore() > result.get(1).getMatchScore(), "Top driver should score higher");
        assertNotNull(topDriver.getMatchReasons());
        assertFalse(topDriver.getMatchReasons().isEmpty());
    }
}

