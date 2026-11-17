package com.drivio.repository;

import com.drivio.entity.DriverDocument;
import com.drivio.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {
    List<DriverDocument> findByDriverProfile(DriverProfile driverProfile);
    List<DriverDocument> findByDriverProfileId(Long driverId);
}

