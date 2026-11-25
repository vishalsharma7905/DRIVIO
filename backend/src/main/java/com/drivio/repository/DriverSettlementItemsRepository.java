package com.drivio.repository;

import com.drivio.entity.DriverSettlementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverSettlementItemsRepository extends JpaRepository<DriverSettlementItem, Long> {
    List<DriverSettlementItem> findBySettlementIdOrderByCreatedAtDesc(Long settlementId);
    List<DriverSettlementItem> findByDriverEarningsId(Long driverEarningsId);
    List<DriverSettlementItem> findByStatusOrderByCreatedAtDesc(String status);
}