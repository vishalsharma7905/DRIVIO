package com.drivio.controller;

import com.drivio.dto.LiveTrackingDto;
import com.drivio.dto.LocationUpdateRequest;
import com.drivio.security.UserPrincipal;
import com.drivio.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Live Tracking", description = "Real-time driver location updates and trip map tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/location")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Driver reports current GPS coordinates")
    public ResponseEntity<LiveTrackingDto> updateLocation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(trackingService.updateLocation(userPrincipal.getId(), request));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get current driver GPS coordinates for an active trip")
    public ResponseEntity<LiveTrackingDto> getLiveTracking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(trackingService.getBookingLiveTracking(bookingId, userPrincipal.getId()));
    }
}

