package com.drivio.repository;

import com.drivio.entity.VehiclePricingConfiguration;
import com.drivio.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiclePricingConfigurationRepository extends JpaRepository<VehiclePricingConfiguration, Long> {
    Optional<VehiclePricingConfiguration> findByVehicleCategory(VehicleType vehicleCategory);
    List<VehiclePricingConfiguration> findAllByOrderByIdAsc();
}
