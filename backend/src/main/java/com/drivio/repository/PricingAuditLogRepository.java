package com.drivio.repository;

import com.drivio.entity.PricingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingAuditLogRepository extends JpaRepository<PricingAuditLog, Long> {
    List<PricingAuditLog> findAllByOrderByChangedAtDesc();
}
