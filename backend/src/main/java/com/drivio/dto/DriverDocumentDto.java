package com.drivio.dto;

import com.drivio.enums.VerificationStatus;
import java.time.LocalDateTime;

public class DriverDocumentDto {
    private Long id;
    private Long driverId;
    private String documentType;
    private String documentUrl;
    private VerificationStatus verificationStatus;
    private Long verifiedByAdminId;
    private String remarks;
    private LocalDateTime createdAt;

    public DriverDocumentDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public Long getVerifiedByAdminId() { return verifiedByAdminId; }
    public void setVerifiedByAdminId(Long verifiedByAdminId) { this.verifiedByAdminId = verifiedByAdminId; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

