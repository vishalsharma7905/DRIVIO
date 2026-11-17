package com.drivio.repository;

import com.drivio.entity.Booking;
import com.drivio.entity.DriverProfile;
import com.drivio.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBooking(Booking booking);
    Optional<Review> findByBookingId(Long bookingId);
    List<Review> findByDriverOrderByCreatedAtDesc(DriverProfile driver);
    List<Review> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.driver.id = :driverId")
    Double calculateAverageRatingForDriver(Long driverId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.driver.id = :driverId")
    Integer countReviewsForDriver(Long driverId);
}

