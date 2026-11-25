package com.drivio.repository;

import com.drivio.entity.DriverSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverSettlementsRepository extends JpaRepository<DriverSettlement, Long> {
    List<DriverSettlement> findByDriverProfileIdOrderByInitiatedAtDesc(Long driverProfileId);
    Optional<DriverSettlement> findTopByDriverProfileIdOrderByInitiatedAtDesc(Long driverProfileId);
    List<DriverSettlement> findByStatusOrderByInitiatedAtDesc(String status);
}