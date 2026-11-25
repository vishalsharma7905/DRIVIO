package com.drivio.repository;

import com.drivio.entity.BookingQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingQuotationRepository extends JpaRepository<BookingQuotation, Long> {
    List<BookingQuotation> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
