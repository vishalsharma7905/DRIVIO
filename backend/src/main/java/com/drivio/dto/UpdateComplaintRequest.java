package com.drivio.dto;

import com.drivio.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateComplaintRequest {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    private String adminNotes;

    public UpdateComplaintRequest() {}

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}

