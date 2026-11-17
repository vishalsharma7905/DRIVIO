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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final RecommendationService recommendationService;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverAvailabilityRepository driverAvailabilityRepository,
            PaymentRepository paymentRepository,
            PricingService pricingService,
            RecommendationService recommendationService,
            NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverAvailabilityRepository = driverAvailabilityRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
        this.recommendationService = recommendationService;
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

        FareEstimateResponse fareEstimate = pricingService.calculateEstimatedFare(fareRequest);

        // Generate unique booking reference (e.g. DRV-20261116-8342)
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        String reference = "DRV-" + dateStr + "-" + randomSuffix;

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

        // Set fare details
        booking.setBaseFare(fareEstimate.getBaseFare());
        booking.setDistanceFare(fareEstimate.getDistanceFare());
        booking.setDurationFare(fareEstimate.getDurationFare());
        booking.setAdditionalCharges(fareEstimate.getAdditionalCharges());
        booking.setTaxAmount(fareEstimate.getTaxAmount());
        booking.setTotalFare(fareEstimate.getTotalEstimatedFare());

        // Driver assignment: either explicit driver from recommendation or automated recommendation
        DriverProfile assignedDriver = null;
        if (request.getRequestedDriverId() != null) {
            assignedDriver = driverProfileRepository.findById(request.getRequestedDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requested driver not found"));
            if (assignedDriver.getVerificationStatus() != VerificationStatus.VERIFIED) {
                throw new BadRequestException("Requested driver is not verified.");
            }
            // Concurrency & Availability check: ensure requested driver is online
            DriverAvailability availability = driverAvailabilityRepository.findByDriverProfileId(assignedDriver.getId()).orElse(null);
            if (availability == null || !availability.isAvailable()) {
                throw new BadRequestException("Requested driver is currently offline or off-duty.");
            }
            // Double-booking check: ensure driver does not already have an active ongoing trip
            if (bookingRepository.hasActiveOngoingTrip(assignedDriver.getId())) {
                throw new BadRequestException("Requested driver is currently engaged in another active trip.");
            }
        } else {
            // Run recommendation algorithm to auto-match top ranked available driver
            RecommendationRequest recRequest = new RecommendationRequest();
            recRequest.setServiceType(request.getServiceType());
            recRequest.setVehicleType(request.getVehicleType());
            recRequest.setPickupLatitude(request.getPickupLatitude());
            recRequest.setPickupLongitude(request.getPickupLongitude());
            recRequest.setScheduledPickupTime(request.getScheduledPickupTime());

            List<DriverRecommendationDto> ranked = recommendationService.recommendDrivers(recRequest);
            if (!ranked.isEmpty()) {
                for (DriverRecommendationDto rec : ranked) {
                    if (!bookingRepository.hasActiveOngoingTrip(rec.getDriverId())) {
                        assignedDriver = driverProfileRepository.findById(rec.getDriverId()).orElse(null);
                        if (assignedDriver != null) break;
                    }
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

        // Notify customer
        notificationService.sendNotification(
                customer.getId(),
                "Booking Confirmed (" + reference + ")",
                "Your booking for " + request.getServiceType().name().replace('_', ' ') + " has been placed. Driver: " +
                        (assignedDriver != null ? assignedDriver.getUser().getFullName() : "Matching in progress"),
                NotificationType.BOOKING_CONFIRMATION,
                savedBooking.getId()
        );

        // Notify driver if assigned
        if (assignedDriver != null) {
            notificationService.sendNotification(
                    assignedDriver.getUser().getId(),
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
        Booking booking = getBookingOrThrow(bookingId);
        DriverProfile driver = getDriverProfileByUser(driverUserId);

        verifyDriverAssignment(booking, driver);

        if (booking.getBookingStatus() != BookingStatus.DRIVER_ACCEPTED && booking.getBookingStatus() != BookingStatus.DRIVER_ARRIVED) {
            throw new BadRequestException("Trip cannot be started from status: " + booking.getBookingStatus());
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
        Booking booking = getBookingOrThrow(bookingId);
        DriverProfile driver = getDriverProfileByUser(driverUserId);

        verifyDriverAssignment(booking, driver);

        if (booking.getBookingStatus() != BookingStatus.TRIP_STARTED) {
            throw new BadRequestException("Trip cannot be completed without being started first");
        }

        booking.setBookingStatus(BookingStatus.TRIP_COMPLETED);
        booking.setActualEndTime(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Update driver stats: total trips, total earnings (75% driver share, 25% platform share)
        BigDecimal driverShare = booking.getTotalFare().multiply(new BigDecimal("0.75"));
        driver.setTotalTrips(driver.getTotalTrips() + 1);
        driver.setTotalEarnings(driver.getTotalEarnings().add(driverShare));
        driverProfileRepository.save(driver);

        // Reset driver availability to ready
        DriverAvailability availability = driverAvailabilityRepository.findByDriverProfileId(driver.getId()).orElse(null);
        if (availability != null) {
            availability.setAvailable(true);
            availability.setStatusNote("Available for new trips");
            driverAvailabilityRepository.save(availability);
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
                "Trip completed! ₹" + driverShare.setScale(2) + " credited to your earnings account.",
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

        Payment p = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (p != null) {
            dto.setPaymentStatus(p.getPaymentStatus());
        } else {
            dto.setPaymentStatus(PaymentStatus.PENDING);
        }

        dto.setCancellationReason(booking.getCancellationReason());
        dto.setSpecialInstructions(booking.getSpecialInstructions());
        dto.setRated(booking.getReview() != null);
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());

        return dto;
    }
}

