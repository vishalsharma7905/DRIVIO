package com.drivio.dto;

import com.drivio.enums.BookingStatus;
import com.drivio.enums.PaymentStatus;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingResponseDto {

    private Long id;
    private String bookingReference;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String driverPhotoUrl;
    private Double driverRating;
    private Double driverLatitude;
    private Double driverLongitude;
    private ServiceType serviceType;
    private VehicleType vehicleType;
    private BookingStatus bookingStatus;
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String destinationAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private LocalDateTime scheduledPickupTime;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal durationFare;
    private BigDecimal additionalCharges;
    private BigDecimal taxAmount;
    private BigDecimal totalFare;
    private PaymentStatus paymentStatus;
    private String cancellationReason;
    private String specialInstructions;
    private String serviceSubtype;
    private Long vehicleId;
    private String vehicleSummary;
    private Integer luggageCount;
    private String pickupOtp;
    private String destinationOtp;
    private Integer odometerStart;
    private Integer odometerEnd;
    private String fuelLevelStart;
    private String fuelLevelEnd;
    private String damageNotesStart;
    private String damageNotesEnd;
    private boolean sosTriggered;
    private LocalDateTime sosTriggeredAt;
    private Integer pricingVersion;
    private String fareSnapshot;
    private Long pricingProfileVersionId;
    private boolean rated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponseDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getDriverPhotoUrl() { return driverPhotoUrl; }
    public void setDriverPhotoUrl(String driverPhotoUrl) { this.driverPhotoUrl = driverPhotoUrl; }

    public Double getDriverRating() { return driverRating; }
    public void setDriverRating(Double driverRating) { this.driverRating = driverRating; }

    public Double getDriverLatitude() { return driverLatitude; }
    public void setDriverLatitude(Double driverLatitude) { this.driverLatitude = driverLatitude; }

    public Double getDriverLongitude() { return driverLongitude; }
    public void setDriverLongitude(Double driverLongitude) { this.driverLongitude = driverLongitude; }

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }

    public Double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }

    public Double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }

    public LocalDateTime getScheduledPickupTime() { return scheduledPickupTime; }
    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) { this.scheduledPickupTime = scheduledPickupTime; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }

    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getDistanceFare() { return distanceFare; }
    public void setDistanceFare(BigDecimal distanceFare) { this.distanceFare = distanceFare; }

    public BigDecimal getDurationFare() { return durationFare; }
    public void setDurationFare(BigDecimal durationFare) { this.durationFare = durationFare; }

    public BigDecimal getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(BigDecimal additionalCharges) { this.additionalCharges = additionalCharges; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public boolean isRated() { return rated; }
    public void setRated(boolean rated) { this.rated = rated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getServiceSubtype() { return serviceSubtype; }
    public void setServiceSubtype(String serviceSubtype) { this.serviceSubtype = serviceSubtype; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleSummary() { return vehicleSummary; }
    public void setVehicleSummary(String vehicleSummary) { this.vehicleSummary = vehicleSummary; }

    public Integer getLuggageCount() { return luggageCount; }
    public void setLuggageCount(Integer luggageCount) { this.luggageCount = luggageCount; }

    public String getPickupOtp() { return pickupOtp; }
    public void setPickupOtp(String pickupOtp) { this.pickupOtp = pickupOtp; }

    public String getDestinationOtp() { return destinationOtp; }
    public void setDestinationOtp(String destinationOtp) { this.destinationOtp = destinationOtp; }

    public Integer getOdometerStart() { return odometerStart; }
    public void setOdometerStart(Integer odometerStart) { this.odometerStart = odometerStart; }

    public Integer getOdometerEnd() { return odometerEnd; }
    public void setOdometerEnd(Integer odometerEnd) { this.odometerEnd = odometerEnd; }

    public String getFuelLevelStart() { return fuelLevelStart; }
    public void setFuelLevelStart(String fuelLevelStart) { this.fuelLevelStart = fuelLevelStart; }

    public String getFuelLevelEnd() { return fuelLevelEnd; }
    public void setFuelLevelEnd(String fuelLevelEnd) { this.fuelLevelEnd = fuelLevelEnd; }

    public String getDamageNotesStart() { return damageNotesStart; }
    public void setDamageNotesStart(String damageNotesStart) { this.damageNotesStart = damageNotesStart; }

    public String getDamageNotesEnd() { return damageNotesEnd; }
    public void setDamageNotesEnd(String damageNotesEnd) { this.damageNotesEnd = damageNotesEnd; }

    public boolean isSosTriggered() { return sosTriggered; }
    public void setSosTriggered(boolean sosTriggered) { this.sosTriggered = sosTriggered; }

    public LocalDateTime getSosTriggeredAt() { return sosTriggeredAt; }
    public void setSosTriggeredAt(LocalDateTime sosTriggeredAt) { this.sosTriggeredAt = sosTriggeredAt; }

    public Integer getPricingVersion() { return pricingVersion; }
    public void setPricingVersion(Integer pricingVersion) { this.pricingVersion = pricingVersion; }

    public String getFareSnapshot() { return fareSnapshot; }
    public void setFareSnapshot(String fareSnapshot) { this.fareSnapshot = fareSnapshot; }

    public Long getPricingProfileVersionId() { return pricingProfileVersionId; }
    public void setPricingProfileVersionId(Long pricingProfileVersionId) { this.pricingProfileVersionId = pricingProfileVersionId; }
}

