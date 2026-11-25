package com.drivio.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "pricing_profile_versions")
public class PricingProfileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "category_key", nullable = false)
    private String categoryKey;

    @Column(name = "vehicle_category")
    private String vehicleCategory;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "per_km_rate", precision = 12, scale = 2)
    private BigDecimal perKmRate = BigDecimal.ZERO;

    @Column(name = "per_minute_rate", precision = 12, scale = 2)
    private BigDecimal perMinuteRate = BigDecimal.ZERO;

    @Column(name = "per_hour_rate", precision = 12, scale = 2)
    private BigDecimal perHourRate = BigDecimal.ZERO;

    @Column(name = "min_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal minFare = BigDecimal.ZERO;

    @Column(name = "waiting_free_minutes", nullable = false)
    private Integer waitingFreeMinutes = 3;

    @Column(name = "waiting_per_minute_rate", precision = 12, scale = 2)
    private BigDecimal waitingPerMinuteRate = BigDecimal.ONE;

    @Column(name = "night_surcharge_percent", precision = 5, scale = 2)
    private BigDecimal nightSurchargePercent = new BigDecimal("10.00");

    @Column(name = "peak_surcharge_percent", precision = 5, scale = 2)
    private BigDecimal peakSurchargePercent = new BigDecimal("20.00");

    @Column(name = "stack_peak_and_night", nullable = false)
    private Boolean stackPeakAndNight = false;

    @Column(name = "cancellation_fee", precision = 12, scale = 2)
    private BigDecimal cancellationFee = BigDecimal.ZERO;

    @Column(name = "platform_commission_percent", precision = 5, scale = 2)
    private BigDecimal platformCommissionPercent = new BigDecimal("25.00");

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("5.00");

    @Column(name = "tax_mode", nullable = false)
    private String taxMode = "EXCLUSIVE";

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Column(name = "jurisdiction", nullable = false)
    private String jurisdiction = "India";

    @Column(name = "gateway_fee_percent", precision = 5, scale = 2)
    private BigDecimal gatewayFeePercent = new BigDecimal("5.00");

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "state", nullable = false)
    private String state = "DRAFT"; // DRAFT/PUBLISHED/RETIRED

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "business_version", nullable = false)
    private Integer businessVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", insertable = false, updatable = false)
    private PricingProfile profile;

    public PricingProfileVersion() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public String getCategoryKey() { return categoryKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }

    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getPerKmRate() { return perKmRate; }
    public void setPerKmRate(BigDecimal perKmRate) { this.perKmRate = perKmRate; }

    public BigDecimal getPerMinuteRate() { return perMinuteRate; }
    public void setPerMinuteRate(BigDecimal perMinuteRate) { this.perMinuteRate = perMinuteRate; }

    public BigDecimal getPerHourRate() { return perHourRate; }
    public void setPerHourRate(BigDecimal perHourRate) { this.perHourRate = perHourRate; }

    public BigDecimal getMinFare() { return minFare; }
    public void setMinFare(BigDecimal minFare) { this.minFare = minFare; }

    public Integer getWaitingFreeMinutes() { return waitingFreeMinutes; }
    public void setWaitingFreeMinutes(Integer waitingFreeMinutes) { this.waitingFreeMinutes = waitingFreeMinutes; }

    public BigDecimal getWaitingPerMinuteRate() { return waitingPerMinuteRate; }
    public void setWaitingPerMinuteRate(BigDecimal waitingPerMinuteRate) { this.waitingPerMinuteRate = waitingPerMinuteRate; }

    public BigDecimal getNightSurchargePercent() { return nightSurchargePercent; }
    public void setNightSurchargePercent(BigDecimal nightSurchargePercent) { this.nightSurchargePercent = nightSurchargePercent; }

    public BigDecimal getPeakSurchargePercent() { return peakSurchargePercent; }
    public void setPeakSurchargePercent(BigDecimal peakSurchargePercent) { this.peakSurchargePercent = peakSurchargePercent; }

    public Boolean getStackPeakAndNight() { return stackPeakAndNight; }
    public void setStackPeakAndNight(Boolean stackPeakAndNight) { this.stackPeakAndNight = stackPeakAndNight; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public BigDecimal getPlatformCommissionPercent() { return platformCommissionPercent; }
    public void setPlatformCommissionPercent(BigDecimal platformCommissionPercent) { this.platformCommissionPercent = platformCommissionPercent; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public String getTaxMode() { return taxMode; }
    public void setTaxMode(String taxMode) { this.taxMode = taxMode; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

    public BigDecimal getGatewayFeePercent() { return gatewayFeePercent; }
    public void setGatewayFeePercent(BigDecimal gatewayFeePercent) { this.gatewayFeePercent = gatewayFeePercent; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(OffsetDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public OffsetDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(OffsetDateTime effectiveTo) { this.effectiveTo = effectiveTo; }

    public Integer getBusinessVersion() { return businessVersion; }
    public void setBusinessVersion(Integer businessVersion) { this.businessVersion = businessVersion; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public PricingProfile getProfile() { return profile; }
    public void setProfile(PricingProfile profile) { this.profile = profile; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}