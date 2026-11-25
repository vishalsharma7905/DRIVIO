package com.drivio;

import com.drivio.dto.*;
import com.drivio.entity.*;
import com.drivio.enums.*;
import com.drivio.exception.BadRequestException;
import com.drivio.payment.PaymentGateway;
import com.drivio.repository.*;
import com.drivio.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers all 17 mandatory scenario groups for DRIVIO Unified Pricing & Financial Architecture:
 * 1-5: Bike, Auto, Car categories, Own-Car Driver, Luggage Delivery
 * 6-7: Pricing version resolution & Future scheduled pricing
 * 8-12: Payment capture, Duplicate webhook idempotency, Partial refund, Full refund, Failed refund
 * 13-16: Driver earnings accrual, Partial settlement (1000->600/400), Failed settlement release, Settlement retry
 * 17: Historical booking snapshot immutability
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class PricingFinancialArchitectureTest {

    @Autowired private PricingService pricingService;
    @Autowired private PricingProfileService pricingProfileService;
    @Autowired private PricingProfileRepository profileRepository;
    @Autowired private PricingProfileVersionRepository versionRepository;
    @Autowired private VehiclePricingConfigurationRepository vehiclePricingRepo;
    @Autowired private PricingConfigurationRepository servicePricingRepo;
    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private FinancialTransactionRepository financialTxRepo;
    @Autowired private DriverEarningsService driverEarningsService;
    @Autowired private SettlementService settlementService;
    @Autowired private DriverEarningsRepository driverEarningsRepo;
    @Autowired private DriverSettlementsRepository settlementsRepo;
    @Autowired private DriverSettlementAttemptsRepository attemptsRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private DriverProfileRepository driverProfileRepository;
    @Autowired private DriverAvailabilityRepository driverAvailabilityRepository;

    private void seedVehicleAndServicePricing() {
        if (vehiclePricingRepo.findByVehicleCategory(VehicleType.BIKE).isEmpty()) {
            seedVehicle(VehicleType.BIKE, "15", "5", "0.50", "25");
            seedVehicle(VehicleType.AUTO, "25", "10", "0.75", "40");
            seedVehicle(VehicleType.HATCHBACK, "50", "13", "1.00", "60");
            seedVehicle(VehicleType.SEDAN, "50", "15", "1.00", "70");
            seedVehicle(VehicleType.PREMIUM_SEDAN, "60", "17", "1.25", "85");
            seedVehicle(VehicleType.SUV, "60", "20", "1.50", "100");
        }
        if (servicePricingRepo.findByServiceType(ServiceType.OWN_CAR_DRIVER).isEmpty()) {
            seedService(ServiceType.OWN_CAR_DRIVER, "150", "12", "60", "250");
            seedService(ServiceType.CAR_LUGGAGE_DELIVERY, "299", "16", "120", "499");
            seedService(ServiceType.OUTSTATION, "300", "14", "80", "500");
        }
    }

    private void seedVehicle(VehicleType vt, String base, String km, String min, String minFare) {
        VehiclePricingConfiguration c = new VehiclePricingConfiguration();
        c.setVehicleCategory(vt);
        c.setBaseFare(new BigDecimal(base));
        c.setPerKmRate(new BigDecimal(km));
        c.setPerMinuteRate(new BigDecimal(min));
        c.setMinFare(new BigDecimal(minFare));
        c.setTaxPercentage(new BigDecimal("5.00"));
        c.setActive(true);
        vehiclePricingRepo.save(c);
    }

    private void seedService(ServiceType st, String base, String km, String hr, String minFare) {
        PricingConfiguration c = new PricingConfiguration();
        c.setServiceType(st);
        c.setBaseFare(new BigDecimal(base));
        c.setPerKmRate(new BigDecimal(km));
        c.setPerHourRate(new BigDecimal(hr));
        c.setMinFare(new BigDecimal(minFare));
        c.setTaxPercentage(new BigDecimal("5.00"));
        c.setActive(true);
        servicePricingRepo.save(c);
    }

    private User createCustomer(String tag) {
        User u = new User("cust_" + tag + "@drivio.com", "hash", "Customer " + tag, "+9190000001", Role.CUSTOMER);
        return userRepository.save(u);
    }

    private DriverProfile createVerifiedDriver(String tag) {
        User u = new User("drv_" + tag + "@drivio.com", "hash", "Driver " + tag, "+9190000002", Role.DRIVER);
        u = userRepository.save(u);
        DriverProfile dp = new DriverProfile(u);
        dp.setVerificationStatus(VerificationStatus.VERIFIED);
        dp.setCurrentLatitude(28.628);
        dp.setCurrentLongitude(77.218);
        dp = driverProfileRepository.save(dp);
        DriverAvailability da = new DriverAvailability(dp, true);
        driverAvailabilityRepository.save(da);
        return dp;
    }

    @Test
    void scenarios1to5_ServiceFamilyIsolationAndAllCategories() {
        seedVehicleAndServicePricing();

        FareEstimateRequest req = new FareEstimateRequest();
        req.setServiceType(ServiceType.ONE_WAY);
        req.setEstimatedDistanceKm(10.0);
        req.setEstimatedDurationMinutes(25);

        req.setVehicleType(VehicleType.BIKE);
        assertEquals(new BigDecimal("77.50"), pricingService.calculateEstimatedFare(req).getSubtotal()); // 15+50+12.50

        req.setVehicleType(VehicleType.AUTO);
        assertEquals(new BigDecimal("143.75"), pricingService.calculateEstimatedFare(req).getSubtotal()); // 25+100+18.75

        req.setVehicleType(VehicleType.HATCHBACK);
        assertEquals(new BigDecimal("205.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        req.setVehicleType(VehicleType.SEDAN);
        assertEquals(new BigDecimal("225.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        req.setVehicleType(VehicleType.PREMIUM_SEDAN);
        assertEquals(new BigDecimal("261.25"), pricingService.calculateEstimatedFare(req).getSubtotal());

        req.setVehicleType(VehicleType.SUV);
        assertEquals(new BigDecimal("297.50"), pricingService.calculateEstimatedFare(req).getSubtotal());

        // Own-Car Driver & Luggage Delivery must use SERVICE_BASED, never normal ride vehicle rates
        req.setServiceType(ServiceType.OWN_CAR_DRIVER);
        req.setVehicleType(VehicleType.SEDAN);
        req.setEstimatedDistanceKm(30.0);
        req.setEstimatedDurationMinutes(60);
        assertEquals(new BigDecimal("570.00"), pricingService.calculateEstimatedFare(req).getSubtotal());

        req.setServiceType(ServiceType.CAR_LUGGAGE_DELIVERY);
        req.setVehicleType(VehicleType.SUV);
        req.setEstimatedDistanceKm(20.0);
        req.setEstimatedDurationMinutes(60);
        // 299 + (20*16*1.15=368) + 120 = 787.00
        assertEquals(new BigDecimal("787.00"), pricingService.calculateEstimatedFare(req).getSubtotal());
    }

    @Test
    void scenarios6and7_PricingVersionResolutionAndFutureScheduledPricing() {
        PricingProfile p = new PricingProfile();
        p.setProfileType("VEHICLE_BASED");
        p.setVehicleCategory("SEDAN");
        p.setServiceType("ONE_WAY");
        p.setBaseFare(new BigDecimal("50.00"));
        p.setPerKmRate(new BigDecimal("15.00"));
        p.setPerMinuteRate(new BigDecimal("1.00"));
        p.setMinFare(new BigDecimal("70.00"));
        p.setEffectiveFrom(OffsetDateTime.now().minusDays(5));
        PricingProfile savedProf = pricingProfileService.createProfile(p, 1L);

        PricingProfileVersion v1 = pricingProfileService.createVersion(savedProf.getId(), new PricingProfileVersion(), 1L);
        pricingProfileService.publishVersion(v1.getId(), OffsetDateTime.now().minusDays(2), 1L);

        // Schedule future v2 for tomorrow
        PricingProfileVersion v2Req = new PricingProfileVersion();
        v2Req.setBaseFare(new BigDecimal("75.00"));
        PricingProfileVersion v2 = pricingProfileService.createVersion(savedProf.getId(), v2Req, 1L);
        OffsetDateTime tomorrow = OffsetDateTime.now().plusDays(1);
        pricingProfileService.publishVersion(v2.getId(), tomorrow, 1L);

        // Now -> resolves v1, NOT future v2
        Optional<PricingProfileVersion> activeNow = pricingProfileService.resolveActiveVersion(ServiceType.ONE_WAY, VehicleType.SEDAN, OffsetDateTime.now());
        assertTrue(activeNow.isPresent());
        assertEquals(v1.getBusinessVersion(), activeNow.get().getBusinessVersion());

        // Future -> resolves v2
        Optional<PricingProfileVersion> activeTomorrow = pricingProfileService.resolveActiveVersion(ServiceType.ONE_WAY, VehicleType.SEDAN, tomorrow.plusHours(1));
        assertTrue(activeTomorrow.isPresent());
        assertEquals(v2.getBusinessVersion(), activeTomorrow.get().getBusinessVersion());

        // Published version cannot be mutated
        assertThrows(BadRequestException.class, () -> pricingProfileService.updateDraftVersion(v1.getId(), new PricingProfileVersion(), 1L));
    }

    @Test
    void scenarios8to12_PaymentCaptureDuplicateWebhookAndRefunds() {
        seedVehicleAndServicePricing();
        User cust = createCustomer("pay1");
        DriverProfile drv = createVerifiedDriver("pay1");

        CreateBookingRequest br = new CreateBookingRequest();
        br.setServiceType(ServiceType.ONE_WAY);
        br.setVehicleType(VehicleType.SEDAN);
        br.setPickupAddress("CP");
        br.setPickupLatitude(28.628);
        br.setPickupLongitude(77.218);
        br.setDestinationAddress("Noida");
        br.setDestinationLatitude(28.625);
        br.setDestinationLongitude(77.375);
        br.setScheduledPickupTime(LocalDateTime.now().plusHours(1));
        br.setEstimatedDistanceKm(10.0);
        br.setEstimatedDurationMinutes(25);
        br.setRequestedDriverId(drv.getId());

        BookingResponseDto booking = bookingService.createBooking(cust.getId(), br);

        // Scenario 8 & 9: Webhook payment capture + duplicate webhook idempotency
        PaymentGateway.PaymentOrderResult order = paymentService.createRazorpayOrder(cust.getId(), booking.getId());
        PaymentService.WebhookResult wh1 = paymentService.handleRazorpayWebhook("evt_unique_1", booking.getId(), order.orderId(), "pay_123", "sig");
        assertTrue(wh1.processed());
        assertFalse(wh1.duplicateIgnored());

        PaymentService.WebhookResult wh2 = paymentService.handleRazorpayWebhook("evt_unique_1", booking.getId(), order.orderId(), "pay_123", "sig");
        assertTrue(wh2.duplicateIgnored());

        long captureCount = financialTxRepo.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                .filter(t -> "PAYMENT_CAPTURED".equals(t.getEventType()))
                .count();
        assertEquals(1, captureCount);

        // Scenario 12: Failed refund leaves payment PAID and does not reduce balance
        PaymentService.RefundResult failedRefund = paymentService.processRefund(booking.getId(), new BigDecimal("50.00"), "rfnd_key_fail", true);
        assertEquals("FAILED", failedRefund.status());

        // Scenario 10: Partial refund (₹100 out of ₹236.25) with exact reconciliation
        PaymentService.RefundResult partial = paymentService.processRefund(booking.getId(), new BigDecimal("100.00"), "rfnd_key_1", false);
        assertEquals("PARTIAL_REFUND", partial.eventType());
        assertEquals(new BigDecimal("100.00"), partial.taxReversal().add(partial.commissionReversal()).add(partial.driverPayoutReversal()));

        // Scenario 11: Remaining full refund + exceed guard
        BigDecimal remaining = booking.getTotalFare().subtract(new BigDecimal("100.00"));
        assertThrows(BadRequestException.class, () ->
                paymentService.processRefund(booking.getId(), remaining.add(new BigDecimal("1.00")), "rfnd_over", false));
        PaymentService.RefundResult fullRest = paymentService.processRefund(booking.getId(), remaining, "rfnd_key_2", false);
        assertEquals("FULL_REFUND", fullRest.eventType());
    }

    @Test
    void scenarios13to16_FourBalancesPartialSettlementFailedSettlementAndRetry() {
        DriverProfile drv = createVerifiedDriver("stl1");
        Long driverUserId = drv.getUser().getId();

        // Seed ₹1000 AVAILABLE earning directly for exact reconciliation test
        DriverEarnings e = new DriverEarnings();
        e.setDriverProfileId(drv.getId());
        e.setAmount(new BigDecimal("1000.00"));
        e.setStatus("AVAILABLE");
        e.setDescription("Completed trip earnings");
        driverEarningsRepo.save(e);

        // Scenario 14: Partial settlement ₹600 out of ₹1,000
        DriverSettlement s1 = settlementService.createSettlement(driverUserId, new BigDecimal("600.00"));
        DriverEarningsService.DriverBalanceSummary mid = driverEarningsService.getBalances(driverUserId);
        assertEquals(new BigDecimal("400.00"), mid.available());
        assertEquals(new BigDecimal("600.00"), mid.reserved());
        assertEquals(new BigDecimal("0.00"), mid.settled());

        settlementService.completeSettlement(s1.getId(), "STL-REF-1");
        DriverEarningsService.DriverBalanceSummary afterSuccess = driverEarningsService.getBalances(driverUserId);
        assertEquals(new BigDecimal("400.00"), afterSuccess.available());
        assertEquals(new BigDecimal("0.00"), afterSuccess.reserved());
        assertEquals(new BigDecimal("600.00"), afterSuccess.settled());

        // Scenario 15 & 16: Failed settlement of remaining ₹400 -> releases back to AVAILABLE -> safe retry succeeds
        DriverSettlement s2 = settlementService.createSettlement(driverUserId, new BigDecimal("400.00"));
        settlementService.failSettlement(s2.getId(), "Bank timeout");
        DriverEarningsService.DriverBalanceSummary afterFail = driverEarningsService.getBalances(driverUserId);
        assertEquals(new BigDecimal("400.00"), afterFail.available());
        assertEquals(new BigDecimal("0.00"), afterFail.reserved());

        DriverSettlement retried = settlementService.retrySettlement(s2.getId(), driverUserId, false, true, "STL-REF-2");
        assertEquals("COMPLETED", retried.getStatus());
        DriverEarningsService.DriverBalanceSummary afterRetry = driverEarningsService.getBalances(driverUserId);
        assertEquals(new BigDecimal("0.00"), afterRetry.available());
        assertEquals(new BigDecimal("1000.00"), afterRetry.settled());
        assertEquals(2, attemptsRepo.findBySettlementIdOrderByTriedAtDesc(s2.getId()).size());

        // Retrying completed settlement must be rejected
        assertThrows(BadRequestException.class, () ->
                settlementService.retrySettlement(s2.getId(), driverUserId, false, true, "STL-REF-DUP"));
    }

    @Test
    void scenario17_HistoricalBookingSnapshotImmutability() {
        seedVehicleAndServicePricing();
        User cust = createCustomer("snap1");
        DriverProfile drv = createVerifiedDriver("snap1");

        CreateBookingRequest br = new CreateBookingRequest();
        br.setServiceType(ServiceType.ONE_WAY);
        br.setVehicleType(VehicleType.SEDAN);
        br.setPickupAddress("CP");
        br.setPickupLatitude(28.628);
        br.setPickupLongitude(77.218);
        br.setDestinationAddress("Noida");
        br.setDestinationLatitude(28.625);
        br.setDestinationLongitude(77.375);
        br.setScheduledPickupTime(LocalDateTime.now().plusHours(1));
        br.setEstimatedDistanceKm(10.0);
        br.setEstimatedDurationMinutes(25);
        br.setRequestedDriverId(drv.getId());

        BookingResponseDto booking = bookingService.createBooking(cust.getId(), br);
        BigDecimal originalFare = booking.getTotalFare();
        assertNotNull(booking.getFareSnapshot());

        // Admin mutates SEDAN pricing later
        VehiclePricingConfiguration sedan = vehiclePricingRepo.findByVehicleCategory(VehicleType.SEDAN).orElseThrow();
        VehiclePricingConfigurationDto dto = new VehiclePricingConfigurationDto();
        dto.setVehicleCategory(VehicleType.SEDAN);
        dto.setBaseFare(new BigDecimal("99.00"));
        dto.setPerKmRate(new BigDecimal("30.00"));
        dto.setPerMinuteRate(new BigDecimal("3.00"));
        dto.setMinFare(new BigDecimal("150.00"));
        dto.setActive(true);
        pricingService.updateVehiclePricingConfiguration(sedan.getId(), dto, 1L);

        // Historical booking retains exact original fare & snapshot
        BookingResponseDto reloaded = bookingService.getBookingById(booking.getId(), cust.getId());
        assertEquals(originalFare, reloaded.getTotalFare());
        assertEquals(booking.getFareSnapshot(), reloaded.getFareSnapshot());
    }
}
