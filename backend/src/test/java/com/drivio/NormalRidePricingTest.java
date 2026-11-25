package com.drivio;

import com.drivio.dto.FareEstimateRequest;
import com.drivio.dto.FareEstimateResponse;
import com.drivio.dto.VehiclePricingConfigurationDto;
import com.drivio.entity.PricingAuditLog;
import com.drivio.entity.VehiclePricingConfiguration;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.exception.BadRequestException;
import com.drivio.repository.PricingAuditLogRepository;
import com.drivio.repository.PricingConfigurationRepository;
import com.drivio.repository.VehiclePricingConfigurationRepository;
import com.drivio.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NormalRidePricingTest {

    @Mock
    private PricingConfigurationRepository pricingConfigRepository;

    @Mock
    private VehiclePricingConfigurationRepository vehiclePricingConfigRepository;

    @Mock
    private PricingAuditLogRepository pricingAuditLogRepository;

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(
                pricingConfigRepository,
                vehiclePricingConfigRepository,
                pricingAuditLogRepository
        );
    }

    private VehiclePricingConfiguration delhiConfig(VehicleType type, String base, String perKm, String perMin, String min) {
        VehiclePricingConfiguration c = new VehiclePricingConfiguration();
        c.setId((long) type.ordinal() + 1);
        c.setVehicleCategory(type);
        c.setBaseFare(new BigDecimal(base));
        c.setPerKmRate(new BigDecimal(perKm));
        c.setPerMinuteRate(new BigDecimal(perMin));
        c.setMinFare(new BigDecimal(min));
        c.setWaitingFreeMinutes(3);
        c.setWaitingPerMinuteRate(new BigDecimal("1.00"));
        c.setNightSurchargePercent(new BigDecimal("10.00"));
        c.setPeakSurchargePercent(new BigDecimal("20.00"));
        c.setCancellationFee(new BigDecimal("35.00"));
        c.setTaxPercentage(new BigDecimal("5.00"));
        c.setVersion(1);
        c.setActive(true);
        return c;
    }

    @Test
    void testRequiredSedanCase_10Km_25Min_Equals225BeforeTax() {
        VehiclePricingConfiguration sedan = delhiConfig(VehicleType.SEDAN, "50", "15", "1.00", "70");
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.SEDAN)).thenReturn(Optional.of(sedan));

        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setVehicleType(VehicleType.SEDAN);
        req.setEstimatedDistanceKm(10.0);
        req.setEstimatedDurationMinutes(25);

        FareEstimateResponse res = pricingService.calculateEstimatedFare(req);
        // 50 + (10 * 15) + (25 * 1) = 50 + 150 + 25 = 225.00
        assertEquals(new BigDecimal("225.00"), res.getSubtotal());
        // 5% GST on 225 = 11.25 -> total 236.25
        assertEquals(new BigDecimal("236.25"), res.getTotalEstimatedFare());
        assertFalse(res.isMinFareApplied());
    }

    @Test
    void testAllSixVehicleCategories() {
        VehiclePricingConfiguration bike = delhiConfig(VehicleType.BIKE, "15", "5", "0.50", "25");
        VehiclePricingConfiguration auto = delhiConfig(VehicleType.AUTO, "25", "10", "0.75", "40");
        VehiclePricingConfiguration hatch = delhiConfig(VehicleType.HATCHBACK, "50", "13", "1.00", "60");
        VehiclePricingConfiguration sedan = delhiConfig(VehicleType.SEDAN, "50", "15", "1.00", "70");
        VehiclePricingConfiguration prem = delhiConfig(VehicleType.PREMIUM_SEDAN, "60", "17", "1.25", "85");
        VehiclePricingConfiguration suv = delhiConfig(VehicleType.SUV, "60", "20", "1.50", "100");

        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.BIKE)).thenReturn(Optional.of(bike));
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.AUTO)).thenReturn(Optional.of(auto));
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.HATCHBACK)).thenReturn(Optional.of(hatch));
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.SEDAN)).thenReturn(Optional.of(sedan));
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.PREMIUM_SEDAN)).thenReturn(Optional.of(prem));
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.SUV)).thenReturn(Optional.of(suv));

        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setEstimatedDistanceKm(10.0);
        req.setEstimatedDurationMinutes(20);

        // BIKE: 15 + 50 + 10 = 75
        req.setVehicleType(VehicleType.BIKE);
        assertEquals(new BigDecimal("75.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        // AUTO: 25 + 100 + 15 = 140
        req.setVehicleType(VehicleType.AUTO);
        assertEquals(new BigDecimal("140.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        // HATCHBACK: 50 + 130 + 20 = 200
        req.setVehicleType(VehicleType.HATCHBACK);
        assertEquals(new BigDecimal("200.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        // SEDAN: 50 + 150 + 20 = 220
        req.setVehicleType(VehicleType.SEDAN);
        assertEquals(new BigDecimal("220.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        // PREMIUM_SEDAN: 60 + 170 + 25 = 255
        req.setVehicleType(VehicleType.PREMIUM_SEDAN);
        assertEquals(new BigDecimal("255.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        // SUV: 60 + 200 + 30 = 290
        req.setVehicleType(VehicleType.SUV);
        assertEquals(new BigDecimal("290.00"), pricingService.calculateEstimatedFare(req).getSubtotal());
    }

    @Test
    void testMinimumFareEnforcement() {
        VehiclePricingConfiguration bike = delhiConfig(VehicleType.BIKE, "15", "5", "0.50", "25");
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.BIKE)).thenReturn(Optional.of(bike));

        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setVehicleType(VehicleType.BIKE);
        req.setEstimatedDistanceKm(1.0);
        req.setEstimatedDurationMinutes(2);

        // 15 + 5 + 1 = 21 < min 25 -> 25
        FareEstimateResponse res = pricingService.calculateEstimatedFare(req);
        assertTrue(res.isMinFareApplied());
        assertEquals(new BigDecimal("25.00"), res.getSubtotal());
    }

    @Test
    void testWaitingCharges_ThreeFreeMinutesThenOnePerMin() {
        VehiclePricingConfiguration sedan = delhiConfig(VehicleType.SEDAN, "50", "15", "1.00", "70");
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.SEDAN)).thenReturn(Optional.of(sedan));

        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setVehicleType(VehicleType.SEDAN);
        req.setEstimatedDistanceKm(10.0);
        req.setEstimatedDurationMinutes(25);
        req.setWaitingMinutes(8); // 3 free -> 5 chargeable * ₹1 = ₹5

        FareEstimateResponse res = pricingService.calculateEstimatedFare(req);
        assertEquals(new BigDecimal("5.00"), res.getWaitingCharges());
        assertEquals(new BigDecimal("230.00"), res.getSubtotal());
    }

    @Test
    void testNightAndPeakSurchargeDoNotStackByDefault() {
        VehiclePricingConfiguration sedan = delhiConfig(VehicleType.SEDAN, "50", "15", "1.00", "70");
        when(vehiclePricingConfigRepository.findByVehicleCategory(VehicleType.SEDAN)).thenReturn(Optional.of(sedan));

        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setVehicleType(VehicleType.SEDAN);
        req.setEstimatedDistanceKm(10.0);
        req.setEstimatedDurationMinutes(25); // base calc = 225.00

        // Night only (10% of 225 = 22.50)
        req.setNightSurchargeApplied(true);
        assertEquals(new BigDecimal("22.50"), pricingService.calculateEstimatedFare(req).getSurchargeAmount());

        // Peak + Night without explicit stack flag -> takes higher (20% of 225 = 45.00, not 30%)
        req.setPeakSurchargeApplied(true);
        assertEquals(new BigDecimal("45.00"), pricingService.calculateEstimatedFare(req).getSurchargeAmount());
    }

    @Test
    void testInvalidNegativeInputsRejected() {
        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setVehicleType(VehicleType.SEDAN);
        req.setEstimatedDistanceKm(-1.0);
        assertThrows(BadRequestException.class, () -> pricingService.calculateEstimatedFare(req));

        req.setEstimatedDistanceKm(5.0);
        req.setWaitingMinutes(-2);
        assertThrows(BadRequestException.class, () -> pricingService.calculateEstimatedFare(req));
    }

    @Test
    void testAdminUpdateIncrementsVersionAndWritesAuditTrail() {
        VehiclePricingConfiguration sedan = delhiConfig(VehicleType.SEDAN, "50", "15", "1.00", "70");
        when(vehiclePricingConfigRepository.findById(4L)).thenReturn(Optional.of(sedan));
        when(vehiclePricingConfigRepository.save(any(VehiclePricingConfiguration.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        VehiclePricingConfigurationDto update = new VehiclePricingConfigurationDto();
        update.setVehicleCategory(VehicleType.SEDAN);
        update.setBaseFare(new BigDecimal("55.00"));
        update.setPerKmRate(new BigDecimal("16.00"));
        update.setPerMinuteRate(new BigDecimal("1.25"));
        update.setMinFare(new BigDecimal("80.00"));
        update.setPeakSurchargePercent(new BigDecimal("25.00"));
        update.setCancellationFee(new BigDecimal("40.00"));
        update.setActive(true);

        VehiclePricingConfigurationDto saved = pricingService.updateVehiclePricingConfiguration(4L, update, 1L);
        assertEquals(2, saved.getVersion());
        assertEquals(new BigDecimal("55.00"), saved.getBaseFare());

        ArgumentCaptor<PricingAuditLog> auditCaptor = ArgumentCaptor.forClass(PricingAuditLog.class);
        verify(pricingAuditLogRepository).save(auditCaptor.capture());
        assertEquals("SEDAN", auditCaptor.getValue().getVehicleCategory());
        assertEquals(2, auditCaptor.getValue().getVersion());
    }

    @Test
    void testPeakSurchargeOutOfRangeRejected() {
        VehiclePricingConfiguration sedan = delhiConfig(VehicleType.SEDAN, "50", "15", "1.00", "70");
        when(vehiclePricingConfigRepository.findById(4L)).thenReturn(Optional.of(sedan));

        VehiclePricingConfigurationDto update = new VehiclePricingConfigurationDto();
        update.setVehicleCategory(VehicleType.SEDAN);
        update.setBaseFare(new BigDecimal("50"));
        update.setPerKmRate(new BigDecimal("15"));
        update.setPerMinuteRate(new BigDecimal("1"));
        update.setMinFare(new BigDecimal("70"));
        update.setPeakSurchargePercent(new BigDecimal("45.00")); // > 30% max

        assertThrows(BadRequestException.class,
                () -> pricingService.updateVehiclePricingConfiguration(4L, update, 1L));
    }
}
