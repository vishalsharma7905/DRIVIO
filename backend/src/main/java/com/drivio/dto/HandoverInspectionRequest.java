package com.drivio.dto;

public class HandoverInspectionRequest {
    private String otp;
    private Integer odometerReading;
    private String fuelLevel;
    private String damageNotes;

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public Integer getOdometerReading() { return odometerReading; }
    public void setOdometerReading(Integer odometerReading) { this.odometerReading = odometerReading; }

    public String getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(String fuelLevel) { this.fuelLevel = fuelLevel; }

    public String getDamageNotes() { return damageNotes; }
    public void setDamageNotes(String damageNotes) { this.damageNotes = damageNotes; }
}
