package com.drivio;

import com.drivio.dto.FareEstimateRequest;
import com.drivio.dto.FareEstimateResponse;
import com.drivio.entity.PricingConfiguration;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.repository.PricingConfigurationRepository;
import com.drivio.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PricingServiceTest {

    @Mock
    private PricingConfigurationRepository pricingConfigRepository;

    @InjectMocks
    private PricingService pricingService;

    private PricingConfiguration ownCarConfig;

    @BeforeEach
    void setUp() {
        ownCarConfig = new PricingConfiguration();
        ownCarConfig.setId(1L);
        ownCarConfig.setServiceType(ServiceType.OWN_CAR_DRIVER);
        ownCarConfig.setBaseFare(new BigDecimal("150.00"));
        ownCarConfig.setPerKmRate(new BigDecimal("12.00"));
        ownCarConfig.setPerHourRate(new BigDecimal("60.00"));
        ownCarConfig.setMinFare(new BigDecimal("250.00"));
        ownCarConfig.setTaxPercentage(new BigDecimal("5.00"));
        ownCarConfig.setActive(true);
    }

    @Test
    void testCalculateEstimatedFare_OwnCarSedan() {
        when(pricingConfigRepository.findByServiceType(ServiceType.OWN_CAR_DRIVER))
                .thenReturn(Optional.of(ownCarConfig));

        FareEstimateRequest request = new FareEstimateRequest();
        request.setServiceType(ServiceType.OWN_CAR_DRIVER);
        request.setVehicleType(VehicleType.SEDAN);
        request.setEstimatedDistanceKm(30.0);
        request.setEstimatedDurationMinutes(60);

        FareEstimateResponse response = pricingService.calculateEstimatedFare(request);

        assertNotNull(response);
        assertEquals(ServiceType.OWN_CAR_DRIVER, response.getServiceType());
        assertEquals(VehicleType.SEDAN, response.getVehicleType());
        assertEquals(new BigDecimal("150.00"), response.getBaseFare());
        // Distance fare: 30 km * 12.00 = 360.00
        assertEquals(new BigDecimal("360.00"), response.getDistanceFare());
        // Duration fare: 1 hr * 60.00 = 60.00
        assertEquals(new BigDecimal("60.00"), response.getDurationFare());
        // Subtotal = 150 + 360 + 60 = 570.00
        assertEquals(new BigDecimal("570.00"), response.getSubtotal());
        // Tax 5% of 570 = 28.50
        assertEquals(new BigDecimal("28.50"), response.getTaxAmount());
        // Total = 598.50
        assertEquals(new BigDecimal("598.50"), response.getTotalEstimatedFare());
    }

    @Test
    void testCalculateEstimatedFare_MinimumFareEnforced() {
        when(pricingConfigRepository.findByServiceType(ServiceType.OWN_CAR_DRIVER))
                .thenReturn(Optional.of(ownCarConfig));

        FareEstimateRequest request = new FareEstimateRequest();
        request.setServiceType(ServiceType.OWN_CAR_DRIVER);
        request.setVehicleType(VehicleType.SEDAN);
        request.setEstimatedDistanceKm(2.0); // 2 * 12 = 24
        request.setEstimatedDurationMinutes(5); // 5/60 * 60 = 5

        FareEstimateResponse response = pricingService.calculateEstimatedFare(request);

        // Subtotal before min fare would be 150 + 24 + 5 = 179.00 < minFare 250.00
        // Min fare should be enforced
        assertEquals(new BigDecimal("250.00"), response.getSubtotal());
        // Tax 5% of 250 = 12.50
        assertEquals(new BigDecimal("12.50"), response.getTaxAmount());
        assertEquals(new BigDecimal("262.50"), response.getTotalEstimatedFare());
    }
}

