package com.drivio.repository;

import com.drivio.entity.PricingConfiguration;
import com.drivio.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingConfigurationRepository extends JpaRepository<PricingConfiguration, Long> {
    Optional<PricingConfiguration> findByServiceType(ServiceType serviceType);
}

