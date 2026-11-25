package com.drivio.repository;

import com.drivio.entity.PricingAuditLogV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingAuditLogV2Repository extends JpaRepository<PricingAuditLogV2, Long> {
    List<PricingAuditLogV2> findAllByOrderByChangedAtDesc();
}
