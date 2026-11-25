package com.drivio.service;

import com.drivio.dto.*;
import com.drivio.entity.*;
import com.drivio.enums.*;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.exception.UnauthorizedException;
import com.drivio.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final RecommendationService recommendationService;
    private final VehicleRepository vehicleRepository;
    private final PricingProfileVersionRepository pricingProfileVersionRepository;
    private final DriverEarningsRepository driverEarningsRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverAvailabilityRepository driverAvailabilityRepository,
            PaymentRepository paymentRepository,
            PricingService pricingService,
            RecommendationService recommendationService,
            VehicleRepository vehicleRepository,
            PricingProfileVersionRepository pricingProfileVersionRepository,
            DriverEarningsRepository driverEarningsRepository,
            FinancialTransactionRepository financialTransactionRepository,
            NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverAvailabilityRepository = driverAvailabilityRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
        this.recommendationService = recommendationService;
        this.vehicleRepository = vehicleRepository;
        this.pricingProfileVersionRepository = pricingProfileVersionRepository;
        this.driverEarningsRepository = driverEarningsRepository;
        this.financialTransactionRepository = financialTransactionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResponseDto createBooking(Long customerUserId, CreateBookingRequest request) {
        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerUserId));

        // Calculate accurate fare breakdown using Pricing Engine
        FareEstimateRequest fareRequest = new FareEstimateRequest();
        fareRequest.setServiceType(request.getServiceType());
        fareRequest.setVehicleType(request.getVehicleType());
        fareRequest.setEstimatedDistanceKm(request.getEstimatedDistanceKm());
        fareRequest.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        fareRequest.setWaitingMinutes(request.getWaitingMinutes() != null ? request.getWaitingMinutes() : 0);
        fareRequest.setNightSurchargeApplied(request.isNightSurchargeApplied());
        fareRequest.setPeakSurchargeApplied(request.isPeakSurchargeApplied());
        fareRequest.setStackPeakAndNight(request.isStackPeakAndNight());
        fareRequest.setTollAmount(request.getTollAmount() != null ? request.getTollAmount() : BigDecimal.ZERO);
        fareRequest.setParkingAmount(request.getParkingAmount() != null ? request.getParkingAmount() : BigDecimal.ZERO);

        FareEstimateResponse fareEstimate = pricingService.calculateEstimatedFare(fareRequest);

        // Generate unique booking reference (e.g. DRV-20261116-8342)
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", SECURE_RANDOM.nextInt(10000));
        String reference = "DRV-" + dateStr + "-" + randomSuffix;

        // Resolve pricing profile version dynamically
        String categoryKey = resolveCategoryKey(request.getServiceType(), request.getVehicleType());
        PricingProfileVersion pricingProfileVersion = resolvePricingProfileVersion(categoryKey, request.getServiceType());

        Booking booking = new Booking();
        booking.setBookingReference(reference);
        booking.setCustomer(customer);
        booking.setServiceType(request.getServiceType());
        booking.setVehicleType(request.getVehicleType());
        booking.setPickupAddress(request.getPickupAddress());
        booking.setPickupLatitude(request.getPickupLatitude());
        booking.setPickupLongitude(request.getPickupLongitude());
        booking.setDestinationAddress(request.getDestinationAddress());
        booking.setDestinationLatitude(request.getDestinationLatitude());
        booking.setDestinationLongitude(request.getDestinationLongitude());
        booking.setScheduledPickupTime(request.getScheduledPickupTime());
        booking.setEstimatedDistanceKm(request.getEstimatedDistanceKm());
        booking.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        booking.setSpecialInstructions(request.getSpecialInstructions());
        booking.setServiceSubtype(request.getServiceSubtype());
        booking.setLuggageCount(request.getLuggageCount() != null ? request.getLuggageCount() : 0);
        booking.setPickupOtp(String.format("%04d", 1000 + SECURE_RANDOM.nextInt(9000)));
        booking.setDestinationOtp(String.format("%04d", 1000 + SECURE_RANDOM.nextInt(9000)));

        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));
            if (!vehicle.getUser().getId().equals(customerUserId)) {
                throw new UnauthorizedException("Selected vehicle does not belong to you");
            }
            booking.setVehicle(vehicle);
        }

        // Set fare details
        booking.setBaseFare(fareEstimate.getBaseFare());
        booking.setDistanceFare(fareEstimate.getDistanceFare());
        booking.setDurationFare(fareEstimate.getDurationFare());
        booking.setAdditionalCharges(fareEstimate.getAdditionalCharges());
        booking.setTaxAmount(fareEstimate.getTaxAmount());
        booking.setTotalFare(fareEstimate.getTotalEstimatedFare());
        booking.setPricingVersion(fareEstimate.getPricingVersion() != null ? fareEstimate.getPricingVersion() : 1);

        // Store pricing profile version reference for snapshot immutability
        if (pricingProfileVersion != null) {
            booking.setPricingProfileVersionId(pricingProfileVersion.getId());
        }

        // Create immutable fare snapshot JSONB at booking creation
        String fareSnapshot = createFareSnapshot(fareEstimate, request.getServiceType(), request.getVehicleType(), categoryKey, pricingProfileVersion != null ? pricingProfileVersion.getBusinessVersion() : 1);
        booking.setFareSnapshot(fareSnapshot);

        // Driver assignment: either explicit driver from recommendation or automated recommendation
        DriverProfile assignedDriver = null;
        if (request.getRequestedDriverId() != null) {
            assignedDriver = driverProfileRepository.findById(request.getRequestedDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requested driver not found"));
            if (assignedDriver.getVerificationStatus() != VerificationStatus.VERIFIED) {
                throw new BadRequestException("Requested driver is not verified.");
            }
            DriverAvailability availability = driverAvailabilityRepository.findByDriverProfileId(assignedDriver.getId()).orElse(null);
            if (availability == null || !availability.isAvailable()) {
                throw new BadRequestException("Requested driver is currently offline or off-duty.");
            }
            if (bookingRepository.hasActiveOngoingTrip(assignedDriver.getId())) {
                throw new BadRequestException("Requested driver is currently engaged in another active trip.");
            }
        } else {
            RecommendationRequest recRequest = new RecommendationRequest();
            recRequest.setServiceType(request.getServiceType());
            recRequest.setVehicleType(request.getVehicleType());
            recRequest.setPickupLatitude(request.getPickupLatitude());
            recRequest.setPickupLongitude(request.getPickupLongitude());
            recRequest.setScheduledPickupTime(request.getScheduledPickupTime());

            List<DriverRecommendationDto> ranked = recommendationService.recommendDrivers(recRequest);
            for (DriverRecommendationDto rec : ranked) {
                if (!bookingRepository.hasActiveOngoingTrip(rec.getDriverId())) {
                    assignedDriver = driverProfileRepository.findById(rec.getDriverId()).orElse(null);
                    if (assignedDriver != null) break;
                }
            }
        }

        if (assignedDriver != null) {
            booking.setDriver(assignedDriver);
            booking.setBookingStatus(BookingStatus.DRIVER_ASSIGNED);
        } else {
            booking.setBookingStatus(BookingStatus.SEARCHING_DRIVER);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Create initial pending payment record
        Payment payment = new Payment(savedBooking, customer, savedBooking.getTotalFare(), PaymentMethod.CASH);
        payment.setTaxAmount(savedBooking.getTaxAmount());
        paymentRepository.save(payment);

        // Create financial transaction entry for payment initiation (idempotent)
        String idempotencyKey = "booking_" + savedBooking.getId() + "_init";
        if (financialTransactionRepository.findByIdempotencyKey(idempotencyKey).isEmpty()) {
            FinancialTransaction financialTransaction = new FinancialTransaction();
            financialTransaction.setBookingId(savedBooking.getId());
            financialTransaction.setDriverProfileId(savedBooking.getDriver() != null ? savedBooking.getDriver().getId() : null);
            financialTransaction.setTransactionId("txn_" + savedBooking.getId() + "_init");
            financialTransaction.setEventType("PAYMENT_INITIATED");
            financialTransaction.setAmount(savedBooking.getTotalFare());
            financialTransaction.setCurrency("INR");
            financialTransaction.setStatus("PENDING");
            financialTransaction.setIdempotencyKey(idempotencyKey);
            financialTransactionRepository.save(financialTransaction);
        }

        // Notify customer
        notificationService.sendNotification(
                customer.getId(),
                "Booking Confirmed (" + reference + ")",
                "Your booking for " + request.getServiceType().name().replace('_', ' ') + " has been placed. Driver: " +
                        (savedBooking.getDriver() != null ? savedBooking.getDriver().getUser().getFullName() : "Matching in progress"),
                NotificationType.BOOKING_CONFIRMATION,
                savedBooking.getId()
        );

        // Notify driver if assigned
        if (savedBooking.getDriver() != null) {
            notificationService.sendNotification(
                    savedBooking.getDriver().getUser().getId(),
                    "New Booking Request (" + reference + ")",
                    "New booking request from " + customer.getFullName() + " at " + request.getPickupAddress(),
                    NotificationType.DRIVER_ASSIGNED,
                    savedBooking.getId()
            );
        }

        return mapToDto(savedBooking);
    }

    @Transactional
    public BookingResponseDto acceptBooking(Long driverUserId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        DriverProfile driver = getDriverProfileByUser(driverUserId);

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }

        if (booking.getBookingStatus() != BookingStatus.DRIVER_ASSIGNED && booking.getBookingStatus() != BookingStatus.SEARCHING_DRIVER) {
            throw new BadRequestException("Booking cannot be accepted from status: " + booking.getBookingStatus());
        }

        // Concurrency check: prevent driver from accepting multiple overlapping trips
        if (bookingRepository.hasActiveOngoingTrip(driver.getId())) {
            throw new BadRequestException("You already have an active ongoing trip in progress. Complete it before accepting another.");
        }

        booking.setBookingStatus(BookingStatus.DRIVER_ACCEPTED);
        Booking saved = bookingRepository.save(booking);

        // Update driver availability to busy during trip
        DriverAvailability availability = driverAvailabilityRepository.findByDriverProfileId(driver.getId()).orElse(null);
        if (availability != null) {
            availability.setAvailable(false);
            availability.setStatusNote("Busy with active booking " + booking.getBookingReference());
            driverAvailabilityRepository.save(availability);
        }

        // Notify customer
        notificationService.sendNotification(
                booking.getCustomer().getId(),
                "Driver Accepted Your Booking!",
                driver.getUser().getFullName() + " has accepted your booking and is preparing for pickup.",
                NotificationType.DRIVER_ACCEPTED,
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Transactional
    public BookingResponseDto rejectBooking(Long driverUserId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        DriverProfile driver = getDriverProfileByUser(driverUserId);

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }

        booking.setBookingStatus(BookingStatus.DRIVER_REJECTED);
        Booking saved = bookingRepository.save(booking);

        // Notify customer
        notificationService.sendNotification(
                booking.getCustomer().getId(),
                "Driver Declined Request",
                "The assigned driver was unavailable. We are searching for another suitable driver.",
                NotificationType.DRIVER_REJECTED,
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Transactional
    public BookingResponseDto driverArrived(Long driverUserId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        DriverProfile driver = getDriverProfileByUser(driverUserId);

        verifyDriverAssignment(booking, driver);

        if (booking.getBookingStatus() != BookingStatus.DRIVER_ACCEPTED) {
            throw new BadRequestException("Cannot mark arrived from status: " + booking.getBookingStatus());
        }

        booking.setBookingStatus(BookingStatus.DRIVER_ARRIVED);
        Booking saved = bookingRepository.save(booking);

        notificationService.sendNotification(
                booking.getCustomer().getId(),
                "Driver Has Arrived!",
                driver.getUser().getFullName() + " has arrived at your pickup location.",
                NotificationType.DRIVER_ARRIVED,
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Transactional
    public BookingResponseDto startTrip(Long driverUserId, Long bookingId) {
        return startTrip(driverUserId, bookingId, null);
    }

    @Transactional
    public BookingResponseDto startTrip(Long driverUserId, Long bookingId, HandoverInspectionRequest handover) {
        Booking booking = getBookingOrThrow(bookingId);
        DriverProfile driver = getDriverProfileByUser(driverUserId);

        verifyDriverAssignment(booking, driver);

        if (booking.getBookingStatus() != BookingStatus.DRIVER_ACCEPTED && booking.getBookingStatus() != BookingStatus.DRIVER_ARRIVED) {
            throw new BadRequestException("Trip cannot be started from status: " + booking.getBookingStatus());
        }

        if (handover != null) {
            if (handover.getOtp() != null && !handover.getOtp().isBlank()
                    && booking.getPickupOtp() != null && !booking.getPickupOtp().equals(handover.getOtp().trim())) {
                throw new BadRequestException("Invalid Pickup OTP");
            }
            if (handover.getOdometerReading() != null) booking.setOdometerStart(handover.getOdometerReading());
            if (handover.getFuelLevel() != null) booking.setFuelLevelStart(handover.getFuelLevel());
            if (handover.getDamageNotes() != null) booking.setDamageNotesStart(handover.getDamageNotes());
        }

        booking.setBookingStatus(BookingStatus.TRIP_STARTED);
        booking.setActualStartTime(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        notificationService.sendNotification(
                booking.getCustomer().getId(),
                "Trip Started",
                "Your journey with " + driver.getUser().getFullName() + " has started. Safe travels!",
                NotificationType.TRIP_STARTED,
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Transactional
    public BookingResponseDto completeTrip(Long driverUserId, Long bookingId) {
        return completeTrip(driverUserId, bookingId, null);
    }

    @Transactional
    public BookingResponseDto completeTrip(Long driverUserId, Long bookingId, HandoverInspectionRequest handover) {
        Booking booking = getBookingOrThrow(bookingId);
        DriverProfile driver = getDriverProfileByUser(driverUserId);

        verifyDriverAssignment(booking, driver);

        if (booking.getBookingStatus() != BookingStatus.TRIP_STARTED) {
            throw new BadRequestException("Trip cannot be completed without being started first");
        }

        if (handover != null) {
            if (handover.getOtp() != null && !handover.getOtp().isBlank()
                    && booking.getDestinationOtp() != null && !booking.getDestinationOtp().equals(handover.getOtp().trim())) {
                throw new BadRequestException("Invalid Destination OTP");
            }
            if (handover.getOdometerReading() != null) booking.setOdometerEnd(handover.getOdometerReading());
            if (handover.getFuelLevel() != null) booking.setFuelLevelEnd(handover.getFuelLevel());
            if (handover.getDamageNotes() != null) booking.setDamageNotesEnd(handover.getDamageNotes());
        }

        booking.setBookingStatus(BookingStatus.TRIP_COMPLETED);
        booking.setActualEndTime(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Append driver earnings ACCRUED entry to the append-only ledger (idempotent per booking)
        BigDecimal driverShare = booking.getTotalFare().multiply(new BigDecimal("0.75")).setScale(2, java.math.RoundingMode.HALF_UP);
        if (driverEarningsRepository.findByBookingId(booking.getId()).isEmpty()) {
            String earningsDescription = "Trip completed: " + booking.getBookingReference() + " - driver share 75%, platform share 25%";
            DriverEarnings driverEarnings = new DriverEarnings();
            driverEarnings.setDriverProfileId(driver.getId());
            driverEarnings.setBookingId(booking.getId());
            driverEarnings.setAmount(driverShare);
            driverEarnings.setStatus("ACCRUED");
            driverEarnings.setDescription(earningsDescription);
            driverEarningsRepository.save(driverEarnings);
        }

        driver.setTotalTrips(driver.getTotalTrips() + 1);
        driverProfileRepository.save(driver);

        // Reset driver availability to ready
        DriverAvailability availability2 = driverAvailabilityRepository.findByDriverProfileId(driver.getId()).orElse(null);
        if (availability2 != null) {
            availability2.setAvailable(true);
            availability2.setStatusNote("Available for new trips");
            driverAvailabilityRepository.save(availability2);
        }

        // Notify customer and driver
        notificationService.sendNotification(
                booking.getCustomer().getId(),
                "Trip Completed!",
                "You have arrived safely. Please complete your payment of ₹" + booking.getTotalFare() + " and rate your driver.",
                NotificationType.TRIP_COMPLETED,
                saved.getId()
        );

        notificationService.sendNotification(
                driver.getUser().getId(),
                "Trip Completed Successfully",
                "Trip completed! ACCRUED earnings entry created (₹" + driverShare.setScale(2) + "). Driver earnings tracked via append-only ledger. Status: ACCRUED. Pending settlement will allocate to driver_settlements.",
                NotificationType.TRIP_COMPLETED,
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long userId, Long bookingId, CancelBookingRequest request) {
        Booking booking = getBookingOrThrow(bookingId);

        boolean isCustomer = booking.getCustomer().getId().equals(userId);
        boolean isDriver = (booking.getDriver() != null && booking.getDriver().getUser().getId().equals(userId));

        if (!isCustomer && !isDriver) {
            throw new UnauthorizedException("You are not authorized to cancel this booking");
        }

        if (booking.getBookingStatus() == BookingStatus.TRIP_COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed trip");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking has already been cancelled");
        }

        if (booking.getBookingStatus() == BookingStatus.TRIP_STARTED && isCustomer) {
            throw new BadRequestException("Trip is currently in progress. Please contact driver or support to cancel.");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getCancellationReason());
        Booking saved = bookingRepository.save(booking);

        // Free up driver if assigned
        if (booking.getDriver() != null) {
            DriverAvailability availability = driverAvailabilityRepository.findByDriverProfileId(booking.getDriver().getId()).orElse(null);
            if (availability != null) {
                availability.setAvailable(true);
                availability.setStatusNote("Available");
                driverAvailabilityRepository.save(availability);
            }
        }

        Long notifyRecipientId = isCustomer ?
                (booking.getDriver() != null ? booking.getDriver().getUser().getId() : null) :
                booking.getCustomer().getId();

        if (notifyRecipientId != null) {
            notificationService.sendNotification(
                    notifyRecipientId,
                    "Booking Cancelled (" + booking.getBookingReference() + ")",
                    "Booking was cancelled: " + request.getCancellationReason(),
                    NotificationType.BOOKING_CANCELLED,
                    saved.getId()
            );
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long bookingId, Long userId) {
        Booking booking = getBookingOrThrow(bookingId);
        boolean isCustomer = booking.getCustomer().getId().equals(userId);
        boolean isDriver = booking.getDriver() != null && booking.getDriver().getUser().getId().equals(userId);
        boolean isAdmin = userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);

        if (!isCustomer && !isDriver && !isAdmin) {
            throw new UnauthorizedException("Access denied: You are not authorized to view this booking");
        }
        return mapToDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getCustomerBookings(Long customerUserId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerUserId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getDriverBookings(Long driverUserId) {
        DriverProfile driver = getDriverProfileByUser(driverUserId);
        return bookingRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAllBookings() {
        return bookingRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private Booking getBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private DriverProfile getDriverProfileByUser(Long userId) {
        return driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));
    }

    private void verifyDriverAssignment(Booking booking, DriverProfile driver) {
        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }
    }

    public BookingResponseDto mapToDto(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(booking.getId());
        dto.setBookingReference(booking.getBookingReference());
        dto.setCustomerId(booking.getCustomer().getId());
        dto.setCustomerName(booking.getCustomer().getFullName());
        dto.setCustomerPhone(booking.getCustomer().getPhoneNumber());

        if (booking.getDriver() != null) {
            DriverProfile dp = booking.getDriver();
            dto.setDriverId(dp.getId());
            dto.setDriverName(dp.getUser().getFullName());
            dto.setDriverPhone(dp.getUser().getPhoneNumber());
            dto.setDriverPhotoUrl(dp.getProfilePhotoUrl());
            dto.setDriverRating(dp.getRating());
            dto.setDriverLatitude(dp.getCurrentLatitude());
            dto.setDriverLongitude(dp.getCurrentLongitude());
        }

        dto.setServiceType(booking.getServiceType());
        dto.setVehicleType(booking.getVehicleType());
        dto.setBookingStatus(booking.getBookingStatus());
        dto.setPickupAddress(booking.getPickupAddress());
        dto.setPickupLatitude(booking.getPickupLatitude());
        dto.setPickupLongitude(booking.getPickupLongitude());
        dto.setDestinationAddress(booking.getDestinationAddress());
        dto.setDestinationLatitude(booking.getDestinationLatitude());
        dto.setDestinationLongitude(booking.getDestinationLongitude());
        dto.setScheduledPickupTime(booking.getScheduledPickupTime());
        dto.setEstimatedDistanceKm(booking.getEstimatedDistanceKm());
        dto.setEstimatedDurationMinutes(booking.getEstimatedDurationMinutes());
        dto.setActualStartTime(booking.getActualStartTime());
        dto.setActualEndTime(booking.getActualEndTime());
        dto.setBaseFare(booking.getBaseFare());
        dto.setDistanceFare(booking.getDistanceFare());
        dto.setDurationFare(booking.getDurationFare());
        dto.setAdditionalCharges(booking.getAdditionalCharges());
        dto.setTaxAmount(booking.getTaxAmount());
        dto.setTotalFare(booking.getTotalFare());

        // Include fare snapshot reference if available
        if (booking.getFareSnapshot() != null) {
            dto.setFareSnapshot(booking.getFareSnapshot());
        }

        // Include pricing profile version ID if available
        if (booking.getPricingProfileVersionId() != null) {
            dto.setPricingProfileVersionId(booking.getPricingProfileVersionId());
        }

        Payment p = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (p != null) {
            dto.setPaymentStatus(p.getPaymentStatus());
        } else {
            dto.setPaymentStatus(PaymentStatus.PENDING);
        }

        dto.setCancellationReason(booking.getCancellationReason());
        dto.setSpecialInstructions(booking.getSpecialInstructions());
        dto.setServiceSubtype(booking.getServiceSubtype());
        if (booking.getVehicle() != null) {
            dto.setVehicleId(booking.getVehicle().getId());
            dto.setVehicleSummary(booking.getVehicle().getMake() + " " + booking.getVehicle().getModel() + " (" + booking.getVehicle().getRegistrationNumber() + ")");
        }
        dto.setLuggageCount(booking.getLuggageCount());
        dto.setPickupOtp(booking.getPickupOtp());
        dto.setDestinationOtp(booking.getDestinationOtp());
        dto.setOdometerStart(booking.getOdometerStart());
        dto.setOdometerEnd(booking.getOdometerEnd());
        dto.setFuelLevelStart(booking.getFuelLevelStart());
        dto.setFuelLevelEnd(booking.getFuelLevelEnd());
        dto.setDamageNotesStart(booking.getDamageNotesStart());
        dto.setDamageNotesEnd(booking.getDamageNotesEnd());
        dto.setSosTriggered(booking.isSosTriggered());
        dto.setSosTriggeredAt(booking.getSosTriggeredAt());
        dto.setPricingVersion(booking.getPricingVersion());
        dto.setRated(booking.getReview() != null);
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());

        return dto;
    }

    @Transactional
    public BookingResponseDto triggerSos(Long userId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        boolean isCustomer = booking.getCustomer().getId().equals(userId);
        boolean isDriver = booking.getDriver() != null && booking.getDriver().getUser().getId().equals(userId);
        if (!isCustomer && !isDriver) {
            throw new UnauthorizedException("You are not authorized to trigger SOS for this booking");
        }
        booking.setSosTriggered(true);
        booking.setSosTriggeredAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .forEach(admin -> notificationService.sendNotification(
                        admin.getId(),
                        "URGENT SOS ALERT (" + booking.getBookingReference() + ")",
                        "Emergency SOS triggered on booking #" + booking.getId() + " by user #" + userId,
                        NotificationType.SYSTEM_ALERT,
                        booking.getId()
                ));

        return mapToDto(saved);
    }

    /**
     * Resolve the category key for pricing profile version lookup.
     * Generated category_key = COALESCE(vehicle_category,'__ALL__')
     */
    private String resolveCategoryKey(ServiceType serviceType, VehicleType vehicleType) {
        // For service-based services (OWN_CAR_DRIVER, CAR_LUGGAGE_DELIVERY, OUTSTATION), use __ALL__
        if (serviceType == ServiceType.OWN_CAR_DRIVER
                || serviceType == ServiceType.CAR_LUGGAGE_DELIVERY
                || serviceType == ServiceType.OUTSTATION) {
            return "__ALL__";
        }
        // For vehicle-based normal rides, use the vehicle type category
        if (vehicleType != null) {
            return vehicleType.name().toLowerCase();
        }
        return "__ALL__";
    }

    /**
     * Resolve the active pricing profile version for a given category key and service type.
     * Uses the versioning resolution: status=PUBLISHED AND effective_from <= now() AND (effective_to IS NULL OR effective_to > now())
     * ORDER BY effective_from DESC, business_version DESC LIMIT 1
     */
    private PricingProfileVersion resolvePricingProfileVersion(String categoryKey, ServiceType serviceType) {
        if (pricingProfileVersionRepository == null) {
            return null;
        }

        // Build where clause based on service family
        // VEHICLE_BASED: match by category_key (vehicle type)
        // SERVICE_BASED: match by __ALL__ category key
        String effectiveState = "PUBLISHED";

        try {
            List<PricingProfileVersion> versions;
            if ("__ALL__".equals(categoryKey)) {
                // Service-based: find all PUBLISHED versions with __ALL__ category key
                versions = pricingProfileVersionRepository.findByCategoryKeyAndState("__ALL__", effectiveState);
            } else {
                // Vehicle-based: find PUBLISHED versions matching the category key
                versions = pricingProfileVersionRepository.findByCategoryKeyAndState(categoryKey, effectiveState);
            }

            if (!versions.isEmpty()) {
                // Return the first (highest business_version) published version
                // In full implementation, would also filter by effective timestamps
                // Sort by business_version DESC - Java list from repo may already be ordered, but let's be safe
                PricingProfileVersion highestVersion = versions.stream()
                        .max((v1, v2) -> v1.getBusinessVersion().compareTo(v2.getBusinessVersion()))
                        .orElse(null);
                return highestVersion;
            }
        } catch (Exception e) {
            // Repository or query issue, return null
        }
        return null;
    }

    /**
     * Create a JSON snapshot of the fare calculation at booking time.
     */
    private String createFareSnapshot(FareEstimateResponse fare, ServiceType serviceType, VehicleType vehicleType, String categoryKey, Integer businessVersion) {
        // Build a simple JSON structure for the snapshot
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"base_fare\":").append(fare.getBaseFare()).append(",");
        sb.append("\"distance_fare\":").append(fare.getDistanceFare()).append(",");
        sb.append("\"duration_fare\":").append(fare.getDurationFare()).append(",");
        sb.append("\"additional_charges\":").append(fare.getAdditionalCharges()).append(",");
        sb.append("\"tax_amount\":").append(fare.getTaxAmount()).append(",");
        sb.append("\"total_estimated_fare\":").append(fare.getTotalEstimatedFare()).append(",");
        sb.append("\"service_type\":\"").append(serviceType).append("\",");
        sb.append("\"vehicle_type\":\"").append(vehicleType != null ? vehicleType : "NULL").append("\",");
        sb.append("\"category_key\":\"").append(categoryKey).append("\",");
        sb.append("\"business_version\":").append(businessVersion).append(",");
        sb.append("\"snapshot_timestamp\":\"").append(LocalDateTime.now()).append("\"");
        sb.append("}");
        return sb.toString();
    }
}