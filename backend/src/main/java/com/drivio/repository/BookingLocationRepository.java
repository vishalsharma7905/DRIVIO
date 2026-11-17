package com.drivio.repository;

import com.drivio.entity.Booking;
import com.drivio.entity.BookingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingLocationRepository extends JpaRepository<BookingLocation, Long> {
    List<BookingLocation> findByBookingOrderByRecordedAtAsc(Booking booking);
    List<BookingLocation> findByBookingIdOrderByRecordedAtAsc(Long bookingId);
}

