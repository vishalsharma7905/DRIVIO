package com.drivio.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelBookingRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;

    public CancelBookingRequest() {}

    public CancelBookingRequest(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}

