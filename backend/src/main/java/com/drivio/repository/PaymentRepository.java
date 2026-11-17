package com.drivio.repository;

import com.drivio.entity.Booking;
import com.drivio.entity.Payment;
import com.drivio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}

