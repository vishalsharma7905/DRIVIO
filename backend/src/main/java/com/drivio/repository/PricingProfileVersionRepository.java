package com.drivio.repository;

import com.drivio.entity.PricingProfileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingProfileVersionRepository extends JpaRepository<PricingProfileVersion, Long> {
    List<PricingProfileVersion> findByCategoryKeyAndState(String categoryKey, String state);
}