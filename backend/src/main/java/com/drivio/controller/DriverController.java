package com.drivio.controller;

import com.drivio.dto.*;
import com.drivio.security.UserPrincipal;
import com.drivio.service.BookingService;
import com.drivio.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Driver", description = "Driver operations, profile, availability, and documents")
public class DriverController {

    private final DriverService driverService;
    private final BookingService bookingService;

    public DriverController(DriverService driverService, BookingService bookingService) {
        this.driverService = driverService;
        this.bookingService = bookingService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Get current driver profile")
    public ResponseEntity<DriverProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(driverService.getProfile(userPrincipal.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Update driver profile and preferences")
    public ResponseEntity<DriverProfileDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UpdateDriverProfileRequest request) {
        return ResponseEntity.ok(driverService.updateProfile(userPrincipal.getId(), request));
    }

    @PutMapping("/availability")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Set driver online availability and shift details")
    public ResponseEntity<DriverAvailabilityDto> updateAvailability(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(driverService.updateAvailability(userPrincipal.getId(), request));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Upload driving license or identity verification document")
    public ResponseEntity<DriverDocumentDto> uploadDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("type") String documentType,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(driverService.uploadDocument(userPrincipal.getId(), documentType, file));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Get driver's uploaded verification documents")
    public ResponseEntity<List<DriverDocumentDto>> getDocuments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(driverService.getDriverDocuments(userPrincipal.getId()));
    }

    @GetMapping("/documents/{documentId}/file")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Securely retrieve an authorized driver verification document")
    public ResponseEntity<Resource> getDocumentFile(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        return driverService.getDocumentFile(userPrincipal.getId(), userPrincipal.getRole(), documentId);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Get driver's bookings history and active trips")
    public ResponseEntity<List<BookingResponseDto>> getBookings(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(bookingService.getDriverBookings(userPrincipal.getId()));
    }
}

