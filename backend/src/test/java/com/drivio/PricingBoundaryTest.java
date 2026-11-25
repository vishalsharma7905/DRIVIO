package com.drivio;

import com.drivio.dto.FareEstimateRequest;
import com.drivio.dto.FareEstimateResponse;
import com.drivio.entity.PricingConfiguration;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.exception.BadRequestException;
import com.drivio.repository.PricingConfigurationRepository;
import com.drivio.service.PricingService;
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
public class PricingBoundaryTest {

    @Mock
    private PricingConfigurationRepository pricingConfigRepository;

    @InjectMocks
    private PricingService pricingService;

    private PricingConfiguration configFor(ServiceType type, boolean active) {
        PricingConfiguration c = new PricingConfiguration();
        c.setId(1L);
        c.setServiceType(type);
        c.setBaseFare(new BigDecimal("200.00"));
        c.setPerKmRate(new BigDecimal("15.00"));
        c.setPerHourRate(new BigDecimal("100.00"));
        c.setMinFare(new BigDecimal("300.00"));
        c.setTaxPercentage(new BigDecimal("5.00"));
        c.setActive(active);
        return c;
    }

    @Test
    void testNegativeDistanceThrowsBadRequest() {
        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setVehicleType(VehicleType.SEDAN);
        req.setEstimatedDistanceKm(-5.0);
        req.setEstimatedDurationMinutes(20);
        assertThrows(BadRequestException.class, () -> pricingService.calculateEstimatedFare(req));
    }

    @Test
    void testInactiveServiceThrowsBadRequest() {
        when(pricingConfigRepository.findByServiceType(ServiceType.HOURLY))
                .thenReturn(Optional.of(configFor(ServiceType.HOURLY, false)));
        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.HOURLY);
        req.setVehicleType(VehicleType.HATCHBACK);
        req.setEstimatedDistanceKm(10.0);
        req.setEstimatedDurationMinutes(60);
        assertThrows(BadRequestException.class, () -> pricingService.calculateEstimatedFare(req));
    }

    @Test
    void testOutstationAllowanceAndLuxuryMultiplier() {
        when(pricingConfigRepository.findByServiceType(ServiceType.OUTSTATION))
                .thenReturn(Optional.of(configFor(ServiceType.OUTSTATION, true)));
        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.OUTSTATION);
        req.setVehicleType(VehicleType.LUXURY);
        req.setEstimatedDistanceKm(100.0);
        req.setEstimatedDurationMinutes(120);

        FareEstimateResponse res = pricingService.calculateEstimatedFare(req);
        // 100 * 15 * 1.35 = 2025.00
        assertEquals(new BigDecimal("2025.00"), res.getDistanceFare());
        assertEquals(new BigDecimal("100.00"), res.getAdditionalCharges());
        assertEquals(new BigDecimal("2525.00"), res.getSubtotal());
    }

    @Test
    void testCarLuggageDeliveryPricing() {
        when(pricingConfigRepository.findByServiceType(ServiceType.CAR_LUGGAGE_DELIVERY))
                .thenReturn(Optional.of(configFor(ServiceType.CAR_LUGGAGE_DELIVERY, true)));
        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.CAR_LUGGAGE_DELIVERY);
        req.setVehicleType(VehicleType.SUV);
        req.setEstimatedDistanceKm(20.0);
        req.setEstimatedDurationMinutes(60);

        FareEstimateResponse res = pricingService.calculateEstimatedFare(req);
        // 20 * 15 * 1.15 = 345.00; base 200 + 345 + 100 = 645.00; 5% tax = 32.25; total = 677.25
        assertEquals(new BigDecimal("345.00"), res.getDistanceFare());
        assertEquals(new BigDecimal("677.25"), res.getTotalEstimatedFare());
    }
}
