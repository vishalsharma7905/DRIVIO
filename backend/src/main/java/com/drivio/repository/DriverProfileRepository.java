package com.drivio.repository;

import com.drivio.entity.DriverProfile;
import com.drivio.entity.User;
import com.drivio.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUser(User user);
    Optional<DriverProfile> findByUserId(Long userId);
    List<DriverProfile> findByVerificationStatus(VerificationStatus status);
    long countByVerificationStatus(VerificationStatus status);

    @Query("SELECT dp FROM DriverProfile dp JOIN dp.availability da " +
           "WHERE dp.verificationStatus = 'VERIFIED' AND da.available = true")
    List<DriverProfile> findAvailableAndVerifiedDrivers();
}

