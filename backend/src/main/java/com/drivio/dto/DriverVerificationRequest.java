package com.drivio.dto;

import com.drivio.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;

public class DriverVerificationRequest {

    @NotNull(message = "Status is required")
    private VerificationStatus status;

    private String remarks;

    public DriverVerificationRequest() {}

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}

