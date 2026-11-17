package com.drivio.repository;

import com.drivio.entity.DriverAvailability;
import com.drivio.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverAvailabilityRepository extends JpaRepository<DriverAvailability, Long> {
    Optional<DriverAvailability> findByDriverProfile(DriverProfile driverProfile);
    Optional<DriverAvailability> findByDriverProfileId(Long driverId);
}

