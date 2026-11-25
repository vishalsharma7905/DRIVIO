package com.drivio.controller;

import com.drivio.dto.BookingResponseDto;
import com.drivio.dto.CancelBookingRequest;
import com.drivio.dto.CreateBookingRequest;
import com.drivio.dto.HandoverInspectionRequest;
import com.drivio.enums.Role;
import com.drivio.security.UserPrincipal;
import com.drivio.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Complete booking lifecycle management")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Create a new booking and assign/search driver")
    public ResponseEntity<BookingResponseDto> createBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponseDto response = bookingService.createBooking(userPrincipal.getId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by id")
    public ResponseEntity<BookingResponseDto> getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(bookingService.getBookingById(id, userPrincipal.getId()));
    }

    @GetMapping("/my")
    @Operation(summary = "Get bookings for currently logged-in user (Customer or Driver)")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal.getRole() == Role.DRIVER) {
            return ResponseEntity.ok(bookingService.getDriverBookings(userPrincipal.getId()));
        } else {
            return ResponseEntity.ok(bookingService.getCustomerBookings(userPrincipal.getId()));
        }
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Driver accepts an assigned booking")
    public ResponseEntity<BookingResponseDto> acceptBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(bookingService.acceptBooking(userPrincipal.getId(), id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Driver declines an assigned booking")
    public ResponseEntity<BookingResponseDto> rejectBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(bookingService.rejectBooking(userPrincipal.getId(), id));
    }

    @PostMapping("/{id}/arrived")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Driver marks arrival at pickup location")
    public ResponseEntity<BookingResponseDto> driverArrived(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(bookingService.driverArrived(userPrincipal.getId(), id));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Driver starts the journey (with optional handover OTP/odometer/fuel)")
    public ResponseEntity<BookingResponseDto> startTrip(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) HandoverInspectionRequest handover) {
        return ResponseEntity.ok(bookingService.startTrip(userPrincipal.getId(), id, handover));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Driver completes the journey (with optional destination OTP/odometer/fuel)")
    public ResponseEntity<BookingResponseDto> completeTrip(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) HandoverInspectionRequest handover) {
        return ResponseEntity.ok(bookingService.completeTrip(userPrincipal.getId(), id, handover));
    }

    @PostMapping("/{id}/sos")
    @Operation(summary = "Trigger emergency SOS alert for an active booking")
    public ResponseEntity<BookingResponseDto> triggerSos(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(bookingService.triggerSos(userPrincipal.getId(), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking with reason")
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CancelBookingRequest request) {
        return ResponseEntity.ok(bookingService.cancelBooking(userPrincipal.getId(), id, request));
    }
}

