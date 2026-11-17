package com.drivio.dto;

import java.math.BigDecimal;

public class AdminDashboardStatsDto {

    private long totalCustomers;
    private long totalDrivers;
    private long verifiedDrivers;
    private long pendingDrivers;
    private long rejectedDrivers;
    private long totalBookings;
    private long activeBookings;
    private long completedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;

    public AdminDashboardStatsDto() {}

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public long getTotalDrivers() { return totalDrivers; }
    public void setTotalDrivers(long totalDrivers) { this.totalDrivers = totalDrivers; }

    public long getVerifiedDrivers() { return verifiedDrivers; }
    public void setVerifiedDrivers(long verifiedDrivers) { this.verifiedDrivers = verifiedDrivers; }

    public long getPendingDrivers() { return pendingDrivers; }
    public void setPendingDrivers(long pendingDrivers) { this.pendingDrivers = pendingDrivers; }

    public long getRejectedDrivers() { return rejectedDrivers; }
    public void setRejectedDrivers(long rejectedDrivers) { this.rejectedDrivers = rejectedDrivers; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getActiveBookings() { return activeBookings; }
    public void setActiveBookings(long activeBookings) { this.activeBookings = activeBookings; }

    public long getCompletedBookings() { return completedBookings; }
    public void setCompletedBookings(long completedBookings) { this.completedBookings = completedBookings; }

    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}

