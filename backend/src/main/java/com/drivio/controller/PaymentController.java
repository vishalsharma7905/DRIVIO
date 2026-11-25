package com.drivio.controller;

import com.drivio.dto.PaymentResponseDto;
import com.drivio.dto.ProcessPaymentRequest;
import com.drivio.payment.PaymentGateway;
import com.drivio.security.UserPrincipal;
import com.drivio.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    public ResponseEntity<PaymentResponseDto> getPaymentByBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId, userPrincipal.getId()));
    }

    @GetMapping("/my")
    @Operation(summary = "Get logged in customer's payment receipts")
    public ResponseEntity<List<PaymentResponseDto>> getMyPayments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.getCustomerPayments(userPrincipal.getId()));
    }

    @PostMapping("/razorpay/order/{bookingId}")
    @Operation(summary = "Create Razorpay sandbox/live payment order for booking")
    public ResponseEntity<PaymentGateway.PaymentOrderResult> createRazorpayOrder(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(userPrincipal.getId(), bookingId));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify Razorpay payment signature and settle booking")
    public ResponseEntity<PaymentResponseDto> verifyRazorpayPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody RazorpayVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyRazorpayPayment(
                userPrincipal.getId(),
                request.bookingId(),
                request.orderId(),
                request.paymentId(),
                request.signature()
        ));
    }

    public record RazorpayVerifyRequest(Long bookingId, String orderId, String paymentId, String signature) {}

    @PostMapping("/webhook/razorpay")
    @Operation(summary = "Idempotent Razorpay webhook endpoint")
    public ResponseEntity<PaymentService.WebhookResult> razorpayWebhook(@RequestBody RazorpayWebhookRequest request) {
        return ResponseEntity.ok(paymentService.handleRazorpayWebhook(
                request.eventId(),
                request.bookingId(),
                request.orderId(),
                request.paymentId(),
                request.signature()
        ));
    }

    @PostMapping("/{bookingId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process full or partial refund with proportional ledger reconciliation (Admin only)")
    public ResponseEntity<PaymentService.RefundResult> processRefund(
            @PathVariable Long bookingId,
            @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.processRefund(
                bookingId,
                request.amount(),
                request.idempotencyKey(),
                Boolean.TRUE.equals(request.simulateFailure())
        ));
    }

    public record RazorpayWebhookRequest(String eventId, Long bookingId, String orderId, String paymentId, String signature) {}
    public record RefundRequest(BigDecimal amount, String idempotencyKey, Boolean simulateFailure) {}
}

