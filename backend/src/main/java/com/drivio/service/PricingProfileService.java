package com.drivio.service;

import com.drivio.entity.PricingAuditLogV2;
import com.drivio.entity.PricingProfile;
import com.drivio.entity.PricingProfileVersion;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.PricingAuditLogV2Repository;
import com.drivio.repository.PricingProfileRepository;
import com.drivio.repository.PricingProfileVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PricingProfileService {

    private final PricingProfileRepository profileRepository;
    private final PricingProfileVersionRepository versionRepository;
    private final PricingAuditLogV2Repository auditLogV2Repository;

    public PricingProfileService(
            PricingProfileRepository profileRepository,
            PricingProfileVersionRepository versionRepository,
            PricingAuditLogV2Repository auditLogV2Repository) {
        this.profileRepository = profileRepository;
        this.versionRepository = versionRepository;
        this.auditLogV2Repository = auditLogV2Repository;
    }

    @Transactional(readOnly = true)
    public List<PricingProfile> listProfiles() {
        return profileRepository.findAll();
    }

    @Transactional
    public PricingProfile createProfile(PricingProfile request, Long adminId) {
        if (request.getProfileType() == null || (!"VEHICLE_BASED".equals(request.getProfileType()) && !"SERVICE_BASED".equals(request.getProfileType()))) {
            throw new BadRequestException("profileType must be VEHICLE_BASED or SERVICE_BASED");
        }
        if ("VEHICLE_BASED".equals(request.getProfileType()) && (request.getVehicleCategory() == null || request.getVehicleCategory().isBlank())) {
            throw new BadRequestException("VEHICLE_BASED profile requires vehicleCategory");
        }
        if ("SERVICE_BASED".equals(request.getProfileType()) && request.getVehicleCategory() != null && !request.getVehicleCategory().isBlank()) {
            throw new BadRequestException("SERVICE_BASED profile must not specify vehicleCategory");
        }
        String categoryKey = "VEHICLE_BASED".equals(request.getProfileType())
                ? request.getVehicleCategory().toLowerCase()
                : request.getServiceType().toLowerCase();
        request.setCategoryKey(categoryKey);
        if (request.getEffectiveFrom() == null) {
            request.setEffectiveFrom(OffsetDateTime.now());
        }
        request.setState("DRAFT");
        PricingProfile saved = profileRepository.save(request);

        recordAudit(saved.getId(), null, adminId, null, "{\"action\":\"CREATE_PROFILE\"}", "CREATE", categoryKey);
        return saved;
    }

    @Transactional
    public PricingProfileVersion createVersion(Long profileId, PricingProfileVersion req, Long adminId) {
        PricingProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing profile not found: " + profileId));

        int nextVersion = versionRepository.findByCategoryKeyAndState(profile.getCategoryKey(), "PUBLISHED").stream()
                .mapToInt(PricingProfileVersion::getBusinessVersion)
                .max()
                .orElse(0) + 1;
        int draftMax = versionRepository.findByCategoryKeyAndState(profile.getCategoryKey(), "DRAFT").stream()
                .mapToInt(PricingProfileVersion::getBusinessVersion)
                .max()
                .orElse(0) + 1;
        int bizVer = Math.max(nextVersion, draftMax);

        PricingProfileVersion v = new PricingProfileVersion();
        v.setProfileId(profile.getId());
        v.setCategoryKey(profile.getCategoryKey());
        v.setVehicleCategory(profile.getVehicleCategory());
        v.setServiceType("SERVICE_BASED".equals(profile.getProfileType()) ? profile.getServiceType() : null);
        v.setBaseFare(req.getBaseFare() != null ? req.getBaseFare() : profile.getBaseFare());
        v.setPerKmRate(req.getPerKmRate() != null ? req.getPerKmRate() : profile.getPerKmRate());
        v.setPerMinuteRate(req.getPerMinuteRate() != null ? req.getPerMinuteRate() : profile.getPerMinuteRate());
        v.setPerHourRate(req.getPerHourRate() != null ? req.getPerHourRate() : profile.getPerHourRate());
        v.setMinFare(req.getMinFare() != null ? req.getMinFare() : profile.getMinFare());
        v.setWaitingFreeMinutes(req.getWaitingFreeMinutes() != null ? req.getWaitingFreeMinutes() : profile.getWaitingFreeMinutes());
        v.setWaitingPerMinuteRate(req.getWaitingPerMinuteRate() != null ? req.getWaitingPerMinuteRate() : profile.getWaitingPerMinuteRate());
        v.setNightSurchargePercent(req.getNightSurchargePercent() != null ? req.getNightSurchargePercent() : profile.getNightSurchargePercent());
        v.setPeakSurchargePercent(req.getPeakSurchargePercent() != null ? req.getPeakSurchargePercent() : profile.getPeakSurchargePercent());
        v.setPlatformCommissionPercent(req.getPlatformCommissionPercent() != null ? req.getPlatformCommissionPercent() : profile.getPlatformCommissionPercent());
        v.setTaxRate(req.getTaxRate() != null ? req.getTaxRate() : profile.getTaxRate());
        v.setState("DRAFT");
        v.setEffectiveFrom(req.getEffectiveFrom() != null ? req.getEffectiveFrom() : OffsetDateTime.now());
        v.setEffectiveTo(null);
        v.setBusinessVersion(bizVer);
        v.setIsActive(true);

        PricingProfileVersion saved = versionRepository.save(v);
        recordAudit(profileId, saved.getId(), adminId, null, "{\"state\":\"DRAFT\",\"version\":" + bizVer + "}", "CREATE", profile.getCategoryKey());
        return saved;
    }

    @Transactional
    public PricingProfileVersion updateDraftVersion(Long versionId, PricingProfileVersion updates, Long adminId) {
        PricingProfileVersion existing = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing version not found: " + versionId));
        if (!"DRAFT".equals(existing.getState())) {
            throw new BadRequestException("Cannot mutate a " + existing.getState() + " pricing version. Create a new version instead.");
        }
        if (updates.getBaseFare() != null) existing.setBaseFare(updates.getBaseFare());
        if (updates.getPerKmRate() != null) existing.setPerKmRate(updates.getPerKmRate());
        if (updates.getPerMinuteRate() != null) existing.setPerMinuteRate(updates.getPerMinuteRate());
        if (updates.getPerHourRate() != null) existing.setPerHourRate(updates.getPerHourRate());
        if (updates.getMinFare() != null) existing.setMinFare(updates.getMinFare());
        if (updates.getNightSurchargePercent() != null) existing.setNightSurchargePercent(updates.getNightSurchargePercent());
        if (updates.getPeakSurchargePercent() != null) existing.setPeakSurchargePercent(updates.getPeakSurchargePercent());
        if (updates.getEffectiveFrom() != null) existing.setEffectiveFrom(updates.getEffectiveFrom());
        existing.setUpdatedAt(OffsetDateTime.now());

        PricingProfileVersion saved = versionRepository.save(existing);
        recordAudit(existing.getProfileId(), saved.getId(), adminId, null, "{\"action\":\"UPDATE_DRAFT\"}", "UPDATE", existing.getCategoryKey());
        return saved;
    }

    @Transactional
    public PricingProfileVersion publishVersion(Long versionId, OffsetDateTime effectiveFrom, Long adminId) {
        PricingProfileVersion target = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing version not found: " + versionId));
        if (!"DRAFT".equals(target.getState())) {
            throw new BadRequestException("Only DRAFT versions can be published");
        }
        OffsetDateTime effective = effectiveFrom != null ? effectiveFrom : target.getEffectiveFrom();
        if (effective == null) effective = OffsetDateTime.now();

        // Check for conflicting future/overlapping published versions
        List<PricingProfileVersion> published = versionRepository.findByCategoryKeyAndState(target.getCategoryKey(), "PUBLISHED");
        for (PricingProfileVersion p : published) {
            if (p.getEffectiveTo() == null && !p.getEffectiveFrom().isBefore(effective)) {
                throw new BadRequestException("Conflicting published version already scheduled at or after " + effective);
            }
            if (p.getEffectiveTo() == null && p.getEffectiveFrom().isBefore(effective)) {
                p.setEffectiveTo(effective);
                p.setUpdatedAt(OffsetDateTime.now());
                versionRepository.save(p);
            }
        }

        target.setState("PUBLISHED");
        target.setEffectiveFrom(effective);
        target.setEffectiveTo(null);
        target.setUpdatedAt(OffsetDateTime.now());
        PricingProfileVersion saved = versionRepository.save(target);

        recordAudit(target.getProfileId(), saved.getId(), adminId, "{\"state\":\"DRAFT\"}", "{\"state\":\"PUBLISHED\",\"effectiveFrom\":\"" + effective + "\"}", "PUBLISH", target.getCategoryKey());
        return saved;
    }

    @Transactional
    public PricingProfileVersion retireVersion(Long versionId, Long adminId) {
        PricingProfileVersion target = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing version not found: " + versionId));
        String oldState = target.getState();
        target.setState("RETIRED");
        target.setIsActive(false);
        target.setEffectiveTo(OffsetDateTime.now());
        target.setUpdatedAt(OffsetDateTime.now());
        PricingProfileVersion saved = versionRepository.save(target);
        recordAudit(target.getProfileId(), saved.getId(), adminId, "{\"state\":\"" + oldState + "\"}", "{\"state\":\"RETIRED\"}", "RETIRE", target.getCategoryKey());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<PricingProfileVersion> resolveActiveVersion(ServiceType serviceType, VehicleType vehicleType, OffsetDateTime atTime) {
        OffsetDateTime ts = atTime != null ? atTime : OffsetDateTime.now();
        boolean isServiceBased = serviceType == ServiceType.OWN_CAR_DRIVER || serviceType == ServiceType.CAR_LUGGAGE_DELIVERY;
        String categoryKey = isServiceBased
                ? serviceType.name().toLowerCase()
                : (vehicleType != null ? vehicleType.name().toLowerCase() : null);

        if (categoryKey == null) return Optional.empty();

        return versionRepository.findByCategoryKeyAndState(categoryKey, "PUBLISHED").stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .filter(v -> !v.getEffectiveFrom().isAfter(ts))
                .filter(v -> v.getEffectiveTo() == null || v.getEffectiveTo().isAfter(ts))
                .max(Comparator.comparing(PricingProfileVersion::getEffectiveFrom)
                        .thenComparing(PricingProfileVersion::getBusinessVersion));
    }

    @Transactional(readOnly = true)
    public List<PricingProfileVersion> getActiveVersions() {
        OffsetDateTime now = OffsetDateTime.now();
        return versionRepository.findAll().stream()
                .filter(v -> "PUBLISHED".equals(v.getState()) && Boolean.TRUE.equals(v.getIsActive()))
                .filter(v -> !v.getEffectiveFrom().isAfter(now) && (v.getEffectiveTo() == null || v.getEffectiveTo().isAfter(now)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PricingProfileVersion> getUpcomingVersions() {
        OffsetDateTime now = OffsetDateTime.now();
        return versionRepository.findAll().stream()
                .filter(v -> "PUBLISHED".equals(v.getState()) && v.getEffectiveFrom().isAfter(now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PricingProfileVersion> getExpiredOrRetiredVersions() {
        OffsetDateTime now = OffsetDateTime.now();
        return versionRepository.findAll().stream()
                .filter(v -> "RETIRED".equals(v.getState()) || (v.getEffectiveTo() != null && !v.getEffectiveTo().isAfter(now)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PricingProfileVersion> getAllVersions() {
        return versionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PricingAuditLogV2> getAuditLogsV2() {
        return auditLogV2Repository.findAllByOrderByChangedAtDesc();
    }

    private void recordAudit(Long profileId, Long versionId, Long adminId, String oldVal, String newVal, String type, String categoryKey) {
        PricingAuditLogV2 log = new PricingAuditLogV2();
        log.setProfileId(profileId);
        log.setProfileVersionId(versionId);
        log.setChangedByAdminId(adminId);
        log.setOldValues(oldVal);
        log.setNewValues(newVal);
        log.setChangeType(type);
        log.setCategoryKey(categoryKey);
        auditLogV2Repository.save(log);
    }
}
