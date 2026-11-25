package com.drivio.repository;

import com.drivio.entity.DriverEarnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverEarningsRepository extends JpaRepository<DriverEarnings, Long> {
    List<DriverEarnings> findByDriverProfileIdOrderByAccruedAtDesc(Long driverProfileId);
    List<DriverEarnings> findByBookingId(Long bookingId);
    List<DriverEarnings> findByStatusOrderByAccruedAtDesc(String status);
    Optional<DriverEarnings> findTopByDriverProfileIdOrderByAccruedAtDesc(Long driverProfileId);
}