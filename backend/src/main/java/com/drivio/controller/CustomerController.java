package com.drivio.controller;

import com.drivio.dto.BookingResponseDto;
import com.drivio.dto.CustomerProfileDto;
import com.drivio.dto.PaymentResponseDto;
import com.drivio.dto.UpdateCustomerProfileRequest;
import com.drivio.security.UserPrincipal;
import com.drivio.service.BookingService;
import com.drivio.service.CustomerService;
import com.drivio.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer", description = "Customer profile and activity endpoints")
public class CustomerController {

    private final CustomerService customerService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    public CustomerController(CustomerService customerService, BookingService bookingService, PaymentService paymentService) {
        this.customerService = customerService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get current customer profile")
    public ResponseEntity<CustomerProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(customerService.getProfile(userPrincipal.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Update customer personal information")
    public ResponseEntity<CustomerProfileDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UpdateCustomerProfileRequest request) {
        return ResponseEntity.ok(customerService.updateProfile(userPrincipal.getId(), request));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get customer's booking history")
    public ResponseEntity<List<BookingResponseDto>> getBookings(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(bookingService.getCustomerBookings(userPrincipal.getId()));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get customer's payment history")
    public ResponseEntity<List<PaymentResponseDto>> getPayments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.getCustomerPayments(userPrincipal.getId()));
    }
}

