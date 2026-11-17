package com.drivio.controller;

import com.drivio.dto.*;
import com.drivio.enums.VerificationStatus;
import com.drivio.security.UserPrincipal;
import com.drivio.service.AdminService;
import com.drivio.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administration dashboard, KYC verifications, complaints, and platform management")
public class AdminController {

    private final AdminService adminService;
    private final BookingService bookingService;

    public AdminController(AdminService adminService, BookingService bookingService) {
        this.adminService = adminService;
        this.bookingService = bookingService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform overview analytics and KPIs")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    @Operation(summary = "List all platform users with roles and status")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Suspend or activate a user account")
    public ResponseEntity<UserDto> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam("active") boolean active) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id, active));
    }

    @GetMapping("/drivers")
    @Operation(summary = "List drivers filtered by status (PENDING, VERIFIED, REJECTED)")
    public ResponseEntity<List<DriverProfileDto>> getDrivers(
            @RequestParam(value = "status", required = false) VerificationStatus status) {
        return ResponseEntity.ok(adminService.getDriversByStatus(status));
    }

    @PutMapping("/drivers/{id}/verify")
    @Operation(summary = "Approve or reject a driver's KYC credentials")
    public ResponseEntity<DriverProfileDto> verifyDriver(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal adminUser,
            @Valid @RequestBody DriverVerificationRequest request) {
        return ResponseEntity.ok(adminService.verifyDriver(adminUser.getId(), id, request));
    }

    @GetMapping("/bookings")
    @Operation(summary = "List all platform bookings with full lifecycle details")
    public ResponseEntity<List<BookingResponseDto>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/complaints")
    @Operation(summary = "List customer/driver complaints")
    public ResponseEntity<List<ComplaintDto>> getAllComplaints() {
        return ResponseEntity.ok(adminService.getAllComplaints());
    }

    @PutMapping("/complaints/{id}")
    @Operation(summary = "Update complaint status and append administrative notes")
    public ResponseEntity<ComplaintDto> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComplaintRequest request) {
        return ResponseEntity.ok(adminService.updateComplaint(id, request));
    }
}

