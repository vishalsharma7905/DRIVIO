package com.drivio.controller;

import com.drivio.dto.PaymentResponseDto;
import com.drivio.dto.ProcessPaymentRequest;
import com.drivio.security.UserPrincipal;
import com.drivio.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Mock payment gateway, cash recording, and receipts")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    @Operation(summary = "Process trip payment via mock gateway or cash")
    public ResponseEntity<PaymentResponseDto> processPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(userPrincipal.getId(), request));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment receipt by booking ID")
    public ResponseEntity<PaymentResponseDto> getPaymentByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get logged in customer's payment receipts")
    public ResponseEntity<List<PaymentResponseDto>> getMyPayments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.getCustomerPayments(userPrincipal.getId()));
    }
}

