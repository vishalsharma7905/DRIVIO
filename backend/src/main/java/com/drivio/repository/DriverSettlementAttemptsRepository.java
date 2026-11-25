package com.drivio.repository;

import com.drivio.entity.DriverSettlementAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverSettlementAttemptsRepository extends JpaRepository<DriverSettlementAttempt, Long> {
    List<DriverSettlementAttempt> findBySettlementIdOrderByTriedAtDesc(Long settlementId);
    Optional<DriverSettlementAttempt> findBySettlementIdAndAttemptNumber(Long settlementId, Integer attemptNumber);
}