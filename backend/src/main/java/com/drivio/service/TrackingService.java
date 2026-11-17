package com.drivio.service;

import com.drivio.dto.LiveTrackingDto;
import com.drivio.dto.LocationUpdateRequest;
import com.drivio.entity.Booking;
import com.drivio.entity.BookingLocation;
import com.drivio.entity.DriverLocationHistory;
import com.drivio.entity.DriverProfile;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.BookingLocationRepository;
import com.drivio.repository.BookingRepository;
import com.drivio.repository.DriverLocationHistoryRepository;
import com.drivio.repository.DriverProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);

    private final DriverProfileRepository driverProfileRepository;
    private final BookingRepository bookingRepository;
    private final BookingLocationRepository bookingLocationRepository;
    private final DriverLocationHistoryRepository driverLocationHistoryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TrackingService(
            DriverProfileRepository driverProfileRepository,
            BookingRepository bookingRepository,
            BookingLocationRepository bookingLocationRepository,
            DriverLocationHistoryRepository driverLocationHistoryRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.driverProfileRepository = driverProfileRepository;
        this.bookingRepository = bookingRepository;
        this.bookingLocationRepository = bookingLocationRepository;
        this.driverLocationHistoryRepository = driverLocationHistoryRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public LiveTrackingDto updateLocation(Long driverUserId, LocationUpdateRequest request) {
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + driverUserId));

        // Update driver's current position
        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driver.setLastLocationUpdate(LocalDateTime.now());
        driverProfileRepository.save(driver);

        // Record history
        DriverLocationHistory history = new DriverLocationHistory(driver, request.getLatitude(), request.getLongitude());
        driverLocationHistoryRepository.save(history);

        LiveTrackingDto trackingDto = new LiveTrackingDto();
        trackingDto.setDriverId(driver.getId());
        trackingDto.setDriverName(driver.getUser().getFullName());
        trackingDto.setDriverPhone(driver.getUser().getPhoneNumber());
        trackingDto.setDriverLatitude(request.getLatitude());
        trackingDto.setDriverLongitude(request.getLongitude());
        trackingDto.setSpeed(request.getSpeed() != null ? request.getSpeed() : 35.0);
        trackingDto.setHeading(request.getHeading() != null ? request.getHeading() : 0.0);
        trackingDto.setTimestamp(LocalDateTime.now());

        // If location is tied to an active booking, record and broadcast
        if (request.getBookingId() != null) {
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

            // Authorization check
            if (booking.getDriver() != null && !booking.getDriver().getId().equals(driver.getId())) {
                throw new BadRequestException("Driver is not assigned to this booking");
            }

            BookingLocation bLocation = new BookingLocation(
                    booking, request.getLatitude(), request.getLongitude(), request.getSpeed(), request.getHeading()
            );
            bookingLocationRepository.save(bLocation);

            trackingDto.setBookingId(booking.getId());
            trackingDto.setBookingReference(booking.getBookingReference());
            trackingDto.setBookingStatus(booking.getBookingStatus());
            trackingDto.setPickupLatitude(booking.getPickupLatitude());
            trackingDto.setPickupLongitude(booking.getPickupLongitude());
            trackingDto.setPickupAddress(booking.getPickupAddress());
            trackingDto.setDestinationLatitude(booking.getDestinationLatitude());
            trackingDto.setDestinationLongitude(booking.getDestinationLongitude());
            trackingDto.setDestinationAddress(booking.getDestinationAddress());

            // Real-time broadcast
            try {
                messagingTemplate.convertAndSend("/topic/tracking/" + booking.getId(), trackingDto);
            } catch (Exception e) {
                logger.warn("WebSocket location broadcast failed for booking {}: {}", booking.getId(), e.getMessage());
            }
        }

        return trackingDto;
    }

    @Transactional(readOnly = true)
    public LiveTrackingDto getBookingLiveTracking(Long bookingId, Long requestingUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        LiveTrackingDto dto = new LiveTrackingDto();
        dto.setBookingId(booking.getId());
        dto.setBookingReference(booking.getBookingReference());
        dto.setBookingStatus(booking.getBookingStatus());
        dto.setPickupLatitude(booking.getPickupLatitude());
        dto.setPickupLongitude(booking.getPickupLongitude());
        dto.setPickupAddress(booking.getPickupAddress());
        dto.setDestinationLatitude(booking.getDestinationLatitude());
        dto.setDestinationLongitude(booking.getDestinationLongitude());
        dto.setDestinationAddress(booking.getDestinationAddress());
        dto.setTimestamp(LocalDateTime.now());

        if (booking.getDriver() != null) {
            DriverProfile driver = booking.getDriver();
            dto.setDriverId(driver.getId());
            dto.setDriverName(driver.getUser().getFullName());
            dto.setDriverPhone(driver.getUser().getPhoneNumber());
            dto.setDriverLatitude(driver.getCurrentLatitude() != null ? driver.getCurrentLatitude() : booking.getPickupLatitude());
            dto.setDriverLongitude(driver.getCurrentLongitude() != null ? driver.getCurrentLongitude() : booking.getPickupLongitude());
            dto.setSpeed(30.0);
            dto.setHeading(90.0);
        }

        return dto;
    }
}

