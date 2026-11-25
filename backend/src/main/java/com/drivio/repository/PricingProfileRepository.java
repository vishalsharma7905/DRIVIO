package com.drivio.repository;

import com.drivio.entity.PricingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingProfileRepository extends JpaRepository<PricingProfile, Long> {
    Optional<PricingProfile> findByCategoryKeyAndIsActive(String categoryKey, boolean isActive);
}