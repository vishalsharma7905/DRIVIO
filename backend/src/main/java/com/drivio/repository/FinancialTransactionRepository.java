package com.drivio.repository;

import com.drivio.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    List<FinancialTransaction> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    List<FinancialTransaction> findByDriverProfileIdOrderByCreatedAtDesc(Long driverProfileId);
    List<FinancialTransaction> findByEventTypeOrderByCreatedAtDesc(String eventType);
    List<FinancialTransaction> findByStatusOrderByCreatedAtDesc(String status);
    Optional<FinancialTransaction> findByIdempotencyKey(String idempotencyKey);
    Optional<FinancialTransaction> findByTransactionId(String transactionId);
    Optional<FinancialTransaction> findByGatewayEventId(String gatewayEventId);
}