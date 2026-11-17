package com.drivio.service;

import com.drivio.dto.FareEstimateRequest;
import com.drivio.dto.FareEstimateResponse;
import com.drivio.dto.PricingConfigurationDto;
import com.drivio.entity.PricingConfiguration;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.PricingConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PricingService {

    private final PricingConfigurationRepository pricingConfigRepository;

    public PricingService(PricingConfigurationRepository pricingConfigRepository) {
        this.pricingConfigRepository = pricingConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<PricingConfigurationDto> getAllPricingConfigurations() {
        return pricingConfigRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PricingConfiguration getConfigurationForService(ServiceType serviceType) {
        return pricingConfigRepository.findByServiceType(serviceType)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing configuration not found for service type: " + serviceType));
    }

    public FareEstimateResponse calculateEstimatedFare(FareEstimateRequest request) {
        PricingConfiguration config = getConfigurationForService(request.getServiceType());

        BigDecimal baseFare = config.getBaseFare();
        BigDecimal perKmRate = config.getPerKmRate();
        BigDecimal perHourRate = config.getPerHourRate();

        // Vehicle type multiplier / surcharge (e.g. LUXURY +30%, SUV +15%)
        BigDecimal vehicleMultiplier = BigDecimal.ONE;
        if (request.getVehicleType() == VehicleType.SUV) {
            vehicleMultiplier = new BigDecimal("1.15");
        } else if (request.getVehicleType() == VehicleType.LUXURY) {
            vehicleMultiplier = new BigDecimal("1.35");
        }

        double distance = request.getEstimatedDistanceKm() != null ? request.getEstimatedDistanceKm() : 0.0;
        int durationMinutes = request.getEstimatedDurationMinutes() != null ? request.getEstimatedDurationMinutes() : 0;

        // Distance fare
        BigDecimal distanceFare = perKmRate.multiply(BigDecimal.valueOf(distance)).multiply(vehicleMultiplier);

        // Duration fare: hours * perHourRate
        double hours = durationMinutes / 60.0;
        BigDecimal durationFare = perHourRate.multiply(BigDecimal.valueOf(hours));

        // Additional charges for night or service type specifics
        BigDecimal additionalCharges = BigDecimal.ZERO;
        if (request.getServiceType() == ServiceType.OUTSTATION) {
            additionalCharges = new BigDecimal("100.00"); // Driver outstation allowance
        }

        BigDecimal subtotal = baseFare.add(distanceFare).add(durationFare).add(additionalCharges);
        if (subtotal.compareTo(config.getMinFare()) < 0) {
            subtotal = config.getMinFare();
        }

        // Tax calculation
        BigDecimal taxPercentage = config.getTaxPercentage();
        BigDecimal taxAmount = subtotal.multiply(taxPercentage).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
        BigDecimal totalEstimatedFare = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        FareEstimateResponse response = new FareEstimateResponse();
        response.setServiceType(request.getServiceType());
        response.setVehicleType(request.getVehicleType());
        response.setEstimatedDistanceKm(distance);
        response.setEstimatedDurationMinutes(durationMinutes);
        response.setBaseFare(baseFare.setScale(2, RoundingMode.HALF_UP));
        response.setPerKmRate(perKmRate.setScale(2, RoundingMode.HALF_UP));
        response.setDistanceFare(distanceFare.setScale(2, RoundingMode.HALF_UP));
        response.setDurationFare(durationFare.setScale(2, RoundingMode.HALF_UP));
        response.setAdditionalCharges(additionalCharges.setScale(2, RoundingMode.HALF_UP));
        response.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        response.setTaxPercentage(taxPercentage);
        response.setTaxAmount(taxAmount);
        response.setTotalEstimatedFare(totalEstimatedFare);

        return response;
    }

    @Transactional
    public PricingConfigurationDto updatePricingConfiguration(Long id, PricingConfigurationDto dto) {
        PricingConfiguration config = pricingConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing configuration not found with id: " + id));

        config.setBaseFare(dto.getBaseFare());
        config.setPerKmRate(dto.getPerKmRate());
        config.setPerHourRate(dto.getPerHourRate());
        config.setMinFare(dto.getMinFare());
        if (dto.getCancellationFee() != null) config.setCancellationFee(dto.getCancellationFee());
        if (dto.getTaxPercentage() != null) config.setTaxPercentage(dto.getTaxPercentage());
        config.setActive(dto.isActive());

        PricingConfiguration updated = pricingConfigRepository.save(config);
        return mapToDto(updated);
    }

    private PricingConfigurationDto mapToDto(PricingConfiguration config) {
        PricingConfigurationDto dto = new PricingConfigurationDto();
        dto.setId(config.getId());
        dto.setServiceType(config.getServiceType());
        dto.setBaseFare(config.getBaseFare());
        dto.setPerKmRate(config.getPerKmRate());
        dto.setPerHourRate(config.getPerHourRate());
        dto.setMinFare(config.getMinFare());
        dto.setCancellationFee(config.getCancellationFee());
        dto.setTaxPercentage(config.getTaxPercentage());
        dto.setActive(config.isActive());
        return dto;
    }
}

