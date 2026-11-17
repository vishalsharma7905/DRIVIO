package com.drivio.dto;

import com.drivio.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PricingConfigurationDto {
    private Long id;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Base fare is required")
    private BigDecimal baseFare;

    @NotNull(message = "Per km rate is required")
    private BigDecimal perKmRate;

    @NotNull(message = "Per hour rate is required")
    private BigDecimal perHourRate;

    @NotNull(message = "Minimum fare is required")
    private BigDecimal minFare;

    private BigDecimal cancellationFee;
    private BigDecimal taxPercentage;
    private boolean active;

    public PricingConfigurationDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getPerKmRate() { return perKmRate; }
    public void setPerKmRate(BigDecimal perKmRate) { this.perKmRate = perKmRate; }

    public BigDecimal getPerHourRate() { return perHourRate; }
    public void setPerHourRate(BigDecimal perHourRate) { this.perHourRate = perHourRate; }

    public BigDecimal getMinFare() { return minFare; }
    public void setMinFare(BigDecimal minFare) { this.minFare = minFare; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal taxPercentage) { this.taxPercentage = taxPercentage; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

