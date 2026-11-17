package com.drivio.repository;

import com.drivio.entity.Booking;
import com.drivio.entity.DriverProfile;
import com.drivio.entity.User;
import com.drivio.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    List<Booking> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findByDriverOrderByCreatedAtDesc(DriverProfile driver);
    List<Booking> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<Booking> findByBookingStatus(BookingStatus status);

    long countByBookingStatus(BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingStatus IN ('DRIVER_ASSIGNED', 'DRIVER_ACCEPTED', 'DRIVER_ARRIVED', 'TRIP_STARTED')")
    long countActiveBookings();

    @Query("SELECT COALESCE(SUM(b.totalFare), 0) FROM Booking b WHERE b.bookingStatus = 'TRIP_COMPLETED'")
    BigDecimal calculateTotalRevenue();
}

