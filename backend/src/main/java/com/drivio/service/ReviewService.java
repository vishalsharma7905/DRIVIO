package com.drivio.service;

import com.drivio.dto.CreateReviewRequest;
import com.drivio.dto.ReviewDto;
import com.drivio.entity.Booking;
import com.drivio.entity.DriverProfile;
import com.drivio.entity.Review;
import com.drivio.entity.User;
import com.drivio.enums.BookingStatus;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.BookingRepository;
import com.drivio.repository.DriverProfileRepository;
import com.drivio.repository.ReviewRepository;
import com.drivio.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    @Transactional
    public ReviewDto createReview(Long customerUserId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerUserId));

        if (!booking.getCustomer().getId().equals(customerUserId)) {
            throw new BadRequestException("You can only review bookings you created");
        }

        if (booking.getBookingStatus() != BookingStatus.TRIP_COMPLETED) {
            throw new BadRequestException("You can only rate completed trips");
        }

        if (booking.getDriver() == null) {
            throw new BadRequestException("No driver was assigned to this booking");
        }

        if (reviewRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new BadRequestException("A review has already been submitted for this booking");
        }

        DriverProfile driver = booking.getDriver();
        Review review = new Review(booking, customer, driver, request.getRating(), request.getReviewText());
        Review saved = reviewRepository.save(review);

        // Recalculate driver's overall rating
        Double avgRating = reviewRepository.calculateAverageRatingForDriver(driver.getId());
        Integer totalReviews = reviewRepository.countReviewsForDriver(driver.getId());

        if (avgRating != null) {
            driver.setRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP).doubleValue());
            driver.setTotalRatingsCount(totalReviews);
            driverProfileRepository.save(driver);
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getDriverReviews(Long driverId) {
        return reviewRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ReviewDto mapToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setBookingId(review.getBooking().getId());
        dto.setCustomerId(review.getCustomer().getId());
        dto.setCustomerName(review.getCustomer().getFullName());
        dto.setDriverId(review.getDriver().getId());
        dto.setDriverName(review.getDriver().getUser().getFullName());
        dto.setRating(review.getRating());
        dto.setReviewText(review.getReviewText());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}

