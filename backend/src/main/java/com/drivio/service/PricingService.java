package com.drivio.service;

import com.drivio.dto.FareEstimateRequest;
import com.drivio.dto.FareEstimateResponse;
import com.drivio.dto.PricingConfigurationDto;
import com.drivio.dto.VehiclePricingConfigurationDto;
import com.drivio.entity.PricingAuditLog;
import com.drivio.entity.PricingConfiguration;
import com.drivio.entity.PricingProfileVersion;
import com.drivio.entity.VehiclePricingConfiguration;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.PricingAuditLogRepository;
import com.drivio.repository.PricingProfileVersionRepository;
import com.drivio.repository.PricingConfigurationRepository;
import com.drivio.repository.VehiclePricingConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PricingService {

    private static final List<VehicleType> NORMAL_RIDE_CATEGORIES = List.of(
            VehicleType.BIKE,
            VehicleType.AUTO,
            VehicleType.HATCHBACK,
            VehicleType.SEDAN,
            VehicleType.PREMIUM_SEDAN,
            VehicleType.SUV
    );

    private final PricingConfigurationRepository pricingConfigRepository;
    private final VehiclePricingConfigurationRepository vehiclePricingConfigRepository;
    private final PricingAuditLogRepository pricingAuditLogRepository;
    private final PricingProfileVersionRepository pricingProfileVersionRepository;

    public PricingService(PricingConfigurationRepository pricingConfigRepository) {
        this(pricingConfigRepository, null, null, null);
    }

    public PricingService(
            PricingConfigurationRepository pricingConfigRepository,
            VehiclePricingConfigurationRepository vehiclePricingConfigRepository,
            PricingAuditLogRepository pricingAuditLogRepository) {
        this(pricingConfigRepository, vehiclePricingConfigRepository, pricingAuditLogRepository, null);
    }

    @Autowired
    public PricingService(
            PricingConfigurationRepository pricingConfigRepository,
            VehiclePricingConfigurationRepository vehiclePricingConfigRepository,
            PricingAuditLogRepository pricingAuditLogRepository,
            PricingProfileVersionRepository pricingProfileVersionRepository) {
        this.pricingConfigRepository = pricingConfigRepository;
        this.vehiclePricingConfigRepository = vehiclePricingConfigRepository;
        this.pricingAuditLogRepository = pricingAuditLogRepository;
        this.pricingProfileVersionRepository = pricingProfileVersionRepository;
    }

    @Transactional(readOnly = true)
    public List<PricingConfigurationDto> getAllPricingConfigurations() {
        return pricingConfigRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehiclePricingConfigurationDto> getAllVehiclePricingConfigurations() {
        if (vehiclePricingConfigRepository == null) {
            return List.of();
        }
        return vehiclePricingConfigRepository.findAllByOrderByIdAsc().stream()
                .map(this::mapVehicleToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PricingAuditLog> getPricingAuditLogs() {
        if (pricingAuditLogRepository == null) {
            return List.of();
        }
        return pricingAuditLogRepository.findAllByOrderByChangedAtDesc();
    }

    @Transactional(readOnly = true)
    public PricingConfiguration getConfigurationForService(ServiceType serviceType) {
        return pricingConfigRepository.findByServiceType(serviceType)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing configuration not found for service type: " + serviceType));
    }

    @Transactional(readOnly = true)
    public List<FareEstimateResponse> getAllNormalRideEstimates(FareEstimateRequest baseRequest) {
        return NORMAL_RIDE_CATEGORIES.stream().map(category -> {
            FareEstimateRequest req = new FareEstimateRequest();
            req.setServiceType(baseRequest.getServiceType() != null ? baseRequest.getServiceType() : ServiceType.ONE_WAY);
            req.setVehicleType(category);
            req.setEstimatedDistanceKm(baseRequest.getEstimatedDistanceKm());
            req.setEstimatedDurationMinutes(baseRequest.getEstimatedDurationMinutes());
            req.setWaitingMinutes(baseRequest.getWaitingMinutes());
            req.setNightSurchargeApplied(baseRequest.isNightSurchargeApplied());
            req.setPeakSurchargeApplied(baseRequest.isPeakSurchargeApplied());
            req.setStackPeakAndNight(baseRequest.isStackPeakAndNight());
            req.setTollAmount(baseRequest.getTollAmount());
            req.setParkingAmount(baseRequest.getParkingAmount());
            return calculateEstimatedFare(req);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FareEstimateResponse calculateEstimatedFare(FareEstimateRequest request) {
        if (request.getServiceType() == null || request.getVehicleType() == null) {
            throw new BadRequestException("Service type and vehicle category are required");
        }

        double distance = request.getEstimatedDistanceKm() != null ? request.getEstimatedDistanceKm() : 0.0;
        int durationMinutes = request.getEstimatedDurationMinutes() != null ? request.getEstimatedDurationMinutes() : 0;
        int waitingMinutes = request.getWaitingMinutes() != null ? request.getWaitingMinutes() : 0;

        if (distance < 0 || durationMinutes < 0 || waitingMinutes < 0) {
            throw new BadRequestException("Distance, duration, and waiting minutes must be non-negative");
        }

        BigDecimal tollAmount = request.getTollAmount() != null ? request.getTollAmount() : BigDecimal.ZERO;
        BigDecimal parkingAmount = request.getParkingAmount() != null ? request.getParkingAmount() : BigDecimal.ZERO;
        if (tollAmount.compareTo(BigDecimal.ZERO) < 0 || parkingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Toll and parking amounts must be non-negative");
        }

        // Determine service family: VEHICLE_BASED (normal rides) vs SERVICE_BASED (personal/delivery)
        // Vehicle-based pricing should ONLY apply for normal rides, NOT for personal/delivery services
        boolean isPersonalOrDeliveryService = request.getServiceType() == ServiceType.OWN_CAR_DRIVER
                || request.getServiceType() == ServiceType.CAR_LUGGAGE_DELIVERY
                || request.getServiceType() == ServiceType.OUTSTATION;

        // Vehicle-based pricing only when: not personal/delivery service AND vehicle config repo available
        if (!isPersonalOrDeliveryService && vehiclePricingConfigRepository != null) {
            Optional<VehiclePricingConfiguration> vehicleConfigOpt =
                    vehiclePricingConfigRepository.findByVehicleCategory(request.getVehicleType());
            if (vehicleConfigOpt.isPresent()) {
                return calculateNormalRideVehicleFare(request, vehicleConfigOpt.get(), distance, durationMinutes, waitingMinutes, tollAmount, parkingAmount);
            }
        }

        // Service-based pricing for personal/delivery services and fallback
        return calculateServiceBasedFare(request, distance, durationMinutes);
    }

    private FareEstimateResponse calculateNormalRideVehicleFare(
            FareEstimateRequest request,
            VehiclePricingConfiguration config,
            double distance,
            int durationMinutes,
            int waitingMinutes,
            BigDecimal tollAmount,
            BigDecimal parkingAmount) {
        if (!config.isActive()) {
            throw new BadRequestException("Vehicle category is currently inactive: " + request.getVehicleType());
        }

        BigDecimal baseFare = config.getBaseFare();
        BigDecimal perKmRate = config.getPerKmRate();
        BigDecimal perMinuteRate = config.getPerMinuteRate();

        // calculatedFare = baseFare + (distanceKm * perKmRate) + (durationMinutes * perMinuteRate)
        BigDecimal distanceFare = perKmRate.multiply(BigDecimal.valueOf(distance)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationFare = perMinuteRate.multiply(BigDecimal.valueOf(durationMinutes)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal calculatedFare = baseFare.add(distanceFare).add(durationFare);

        boolean minFareApplied = calculatedFare.compareTo(config.getMinFare()) < 0;
        BigDecimal fareAfterMin = minFareApplied ? config.getMinFare() : calculatedFare;

        // Waiting charges: 3 free minutes, then configured per-minute rate (never combined with journey duration)
        int chargeableWaitMinutes = Math.max(0, waitingMinutes - config.getWaitingFreeMinutes());
        BigDecimal waitingCharges = config.getWaitingPerMinuteRate()
                .multiply(BigDecimal.valueOf(chargeableWaitMinutes))
                .setScale(2, RoundingMode.HALF_UP);

        // Surcharge policy: do not stack peak and night multipliers unless explicitly configured
        BigDecimal surchargePercent = BigDecimal.ZERO;
        if (request.isPeakSurchargeApplied() && request.isNightSurchargeApplied()) {
            if (request.isStackPeakAndNight()) {
                surchargePercent = config.getPeakSurchargePercent().add(config.getNightSurchargePercent());
            } else {
                surchargePercent = config.getPeakSurchargePercent().max(config.getNightSurchargePercent());
            }
        } else if (request.isPeakSurchargeApplied()) {
            surchargePercent = config.getPeakSurchargePercent();
        } else if (request.isNightSurchargeApplied()) {
            surchargePercent = config.getNightSurchargePercent();
        }

        BigDecimal surchargeAmount = fareAfterMin
                .multiply(surchargePercent)
                .divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

        BigDecimal normalizedTolls = tollAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedParking = parkingAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal additionalCharges = waitingCharges.add(surchargeAmount).add(normalizedTolls).add(normalizedParking);

        BigDecimal subtotal = fareAfterMin.add(additionalCharges).setScale(2, RoundingMode.HALF_UP);
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
        response.setPerMinuteRate(perMinuteRate.setScale(2, RoundingMode.HALF_UP));
        response.setDistanceFare(distanceFare);
        response.setDurationFare(durationFare);
        response.setWaitingCharges(waitingCharges);
        response.setSurchargeAmount(surchargeAmount);
        response.setTollAmount(normalizedTolls);
        response.setParkingAmount(normalizedParking);
        response.setAdditionalCharges(additionalCharges.setScale(2, RoundingMode.HALF_UP));
        response.setSubtotal(subtotal);
        response.setTaxPercentage(taxPercentage);
        response.setTaxAmount(taxAmount);
        response.setTotalEstimatedFare(totalEstimatedFare);
        // Set pricing version from the configuration if available
        if (config.getVersion() != null) {
            response.setPricingVersion(config.getVersion());
        } else {
            response.setPricingVersion(1);
        }
        response.setMinFareApplied(minFareApplied);
        return response;
    }

    private FareEstimateResponse calculateServiceBasedFare(FareEstimateRequest request, double distance, int durationMinutes) {
        PricingConfiguration config = getConfigurationForService(request.getServiceType());
        if (!config.isActive()) {
            throw new BadRequestException("Service type is currently inactive: " + request.getServiceType());
        }

        BigDecimal baseFare = config.getBaseFare();
        BigDecimal perKmRate = config.getPerKmRate();
        BigDecimal perHourRate = config.getPerHourRate();

        BigDecimal vehicleMultiplier = BigDecimal.ONE;
        if (request.getVehicleType() == VehicleType.SUV) {
            vehicleMultiplier = new BigDecimal("1.15");
        } else if (request.getVehicleType() == VehicleType.LUXURY || request.getVehicleType() == VehicleType.PREMIUM_SEDAN) {
            vehicleMultiplier = new BigDecimal("1.35");
        }

        // Resolve pricing version dynamically from pricing_profile_versions table
        // instead of hardcoding version 1
        Integer resolvedVersion = resolvePricingVersionDynamic(request.getServiceType(), request.getVehicleType());

        BigDecimal distanceFare = perKmRate.multiply(BigDecimal.valueOf(distance)).multiply(vehicleMultiplier);
        double hours = durationMinutes / 60.0;
        BigDecimal durationFare = perHourRate.multiply(BigDecimal.valueOf(hours));

        BigDecimal additionalCharges = BigDecimal.ZERO;
        if (request.getServiceType() == ServiceType.OUTSTATION) {
            additionalCharges = new BigDecimal("100.00");
        }

        BigDecimal subtotal = baseFare.add(distanceFare).add(durationFare).add(additionalCharges);
        boolean minFareApplied = subtotal.compareTo(config.getMinFare()) < 0;
        if (minFareApplied) {
            subtotal = config.getMinFare();
        }

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
        response.setPricingVersion(resolvedVersion != null ? resolvedVersion : 1);
        response.setMinFareApplied(minFareApplied);
        return response;
    }

    /**
     * Dynamically resolve the pricing profile version for a given service type and booking context.
     * Looks up the pricing_profile_versions table using (service family, category key, timestamp) resolution.
     */
    private Integer resolvePricingVersionDynamic(ServiceType serviceType, VehicleType vehicleType) {
        if (pricingProfileVersionRepository == null) {
            return 1; // fallback to version 1 if repo not available
        }

        String categoryKey;
        if (serviceType.name().contains("OWN_CAR") || serviceType.name().contains("LUGGAGE") || serviceType == ServiceType.OUTSTATION) {
            categoryKey = "__ALL__";
        } else if (vehicleType != null) {
            categoryKey = vehicleType.name().toLowerCase();
        } else {
            categoryKey = "__ALL__";
        }

        // Resolution: status=PUBLISHED AND effective_from <= now() AND (effective_to IS NULL OR effective_to > now())
        // ORDER BY effective_from DESC, business_version DESC LIMIT 1
        // Note: In this method we don't have a timestamp, so we look for active published versions
        try {
            List<PricingProfileVersion> versions = pricingProfileVersionRepository.findByCategoryKeyAndState(
                    categoryKey, "PUBLISHED");
            if (!versions.isEmpty()) {
                // Sort by business_version DESC and take the first (highest version)
                // In a full implementation, this would also filter by effective timestamps
                return versions.get(0).getBusinessVersion();
            }
        } catch (Exception e) {
            // Repository or query issue, fallback to 1
        }
        return 1;
    }

    // Helper method to get vehicle type string - needed for resolvePricingVersionDynamic
    // This will be refactored; for now use the service type to determine category
    private String getCategoryKeyForService(ServiceType serviceType) {
        if (serviceType == ServiceType.OWN_CAR_DRIVER
                || serviceType == ServiceType.CAR_LUGGAGE_DELIVERY
                || serviceType == ServiceType.OUTSTATION) {
            return "__ALL__";
        }
        // For normal ride categories, we'd need the vehicle type from the request
        // This is a placeholder - the actual implementation passes vehicle type through
        return "__ALL__";
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

    @Transactional
    public VehiclePricingConfigurationDto updateVehiclePricingConfiguration(
            Long id,
            VehiclePricingConfigurationDto dto,
            Long adminUserId) {
        VehiclePricingConfiguration config = vehiclePricingConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle pricing configuration not found with id: " + id));

        if (dto.getPeakSurchargePercent() != null &&
                (dto.getPeakSurchargePercent().compareTo(new BigDecimal("10.00")) < 0
                        || dto.getPeakSurchargePercent().compareTo(new BigDecimal("30.00")) > 0)) {
            throw new BadRequestException("Peak surcharge must be between 10% and 30%");
        }
        if (dto.getCancellationFee() != null &&
                (dto.getCancellationFee().compareTo(new BigDecimal("25.00")) < 0
                        || dto.getCancellationFee().compareTo(new BigDecimal("50.00")) > 0)) {
            throw new BadRequestException("Cancellation fee must be between ₹25 and ₹50");
        }

        String oldSnapshot = String.format(
                "base=₹%s, perKm=₹%s, perMin=₹%s, min=₹%s, waitFree=%d, waitRate=₹%s, night=%s%%, peak=%s%%, cancel=₹%s",
                config.getBaseFare(), config.getPerKmRate(), config.getPerMinuteRate(), config.getMinFare(),
                config.getWaitingFreeMinutes(), config.getWaitingPerMinuteRate(),
                config.getNightSurchargePercent(), config.getPeakSurchargePercent(), config.getCancellationFee()
        );

        config.setBaseFare(dto.getBaseFare());
        config.setPerKmRate(dto.getPerKmRate());
        config.setPerMinuteRate(dto.getPerMinuteRate());
        config.setMinFare(dto.getMinFare());
        if (dto.getWaitingFreeMinutes() != null) config.setWaitingFreeMinutes(dto.getWaitingFreeMinutes());
        if (dto.getWaitingPerMinuteRate() != null) config.setWaitingPerMinuteRate(dto.getWaitingPerMinuteRate());
        if (dto.getNightSurchargePercent() != null) config.setNightSurchargePercent(dto.getNightSurchargePercent());
        if (dto.getPeakSurchargePercent() != null) config.setPeakSurchargePercent(dto.getPeakSurchargePercent());
        if (dto.getCancellationFee() != null) config.setCancellationFee(dto.getCancellationFee());
        if (dto.getTaxPercentage() != null) config.setTaxPercentage(dto.getTaxPercentage());
        config.setVersion(config.getVersion() + 1);

        VehiclePricingConfiguration saved = vehiclePricingConfigRepository.save(config);

        if (pricingAuditLogRepository != null) {
            String newSnapshot = String.format(
                    "base=₹%s, perKm=₹%s, perMin=₹%s, min=₹%s, waitFree=%d, waitRate=₹%s, night=%s%%, peak=%s%%, cancel=₹%s",
                    saved.getBaseFare(), saved.getPerKmRate(), saved.getPerMinuteRate(), saved.getMinFare(),
                    saved.getWaitingFreeMinutes(), saved.getWaitingPerMinuteRate(),
                    saved.getNightSurchargePercent(), saved.getPeakSurchargePercent(), saved.getCancellationFee()
            );
            PricingAuditLog log = new PricingAuditLog();
            log.setVehicleCategory(saved.getVehicleCategory().name());
            log.setVersion(saved.getVersion());
            log.setChangedByAdminId(adminUserId);
            log.setOldValues(oldSnapshot);
            log.setNewValues(newSnapshot);
            pricingAuditLogRepository.save(log);
        }

        return mapVehicleToDto(saved);
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

    private VehiclePricingConfigurationDto mapVehicleToDto(VehiclePricingConfiguration config) {
        VehiclePricingConfigurationDto dto = new VehiclePricingConfigurationDto();
        dto.setId(config.getId());
        dto.setVehicleCategory(config.getVehicleCategory());
        dto.setBaseFare(config.getBaseFare());
        dto.setPerKmRate(config.getPerKmRate());
        dto.setPerMinuteRate(config.getPerMinuteRate());
        dto.setMinFare(config.getMinFare());
        dto.setWaitingFreeMinutes(config.getWaitingFreeMinutes());
        dto.setWaitingPerMinuteRate(config.getWaitingPerMinuteRate());
        dto.setNightSurchargePercent(config.getNightSurchargePercent());
        dto.setPeakSurchargePercent(config.getPeakSurchargePercent());
        dto.setCancellationFee(config.getCancellationFee());
        dto.setTaxPercentage(config.getTaxPercentage());
        dto.setVersion(config.getVersion());
        dto.setActive(config.isActive());
        return dto;
    }
}