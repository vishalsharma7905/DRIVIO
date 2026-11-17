package com.drivio.repository;

import com.drivio.entity.DriverLocationHistory;
import com.drivio.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverLocationHistoryRepository extends JpaRepository<DriverLocationHistory, Long> {
    List<DriverLocationHistory> findByDriverProfileOrderByRecordedAtDesc(DriverProfile driverProfile);
    List<DriverLocationHistory> findByDriverProfileIdOrderByRecordedAtDesc(Long driverId);
}

