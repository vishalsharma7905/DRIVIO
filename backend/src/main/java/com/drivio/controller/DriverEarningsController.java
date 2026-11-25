package com.drivio.controller;

import com.drivio.entity.DriverSettlement;
import com.drivio.entity.DriverSettlementAttempt;
import com.drivio.enums.Role;
import com.drivio.security.UserPrincipal;
import com.drivio.service.DriverEarningsService;
import com.drivio.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/drivers/earnings")
@Tag(name = "Driver Earnings & Settlements", description = "Four-balance earnings ledger and settlement lifecycle")
public class DriverEarningsController {

    private final DriverEarningsService earningsService;
    private final SettlementService settlementService;

    public DriverEarningsController(DriverEarningsService earningsService, SettlementService settlementService) {
        this.earningsService = earningsService;
        this.settlementService = settlementService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Get driver's four balances (Accrued, Available, Reserved, Settled) and ledger history")
    public ResponseEntity<DriverEarningsService.DriverBalanceSummary> getSummary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(earningsService.getBalances(principal.getId()));
    }

    @GetMapping("/settlements")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "List driver settlement requests")
    public ResponseEntity<List<DriverSettlement>> getSettlements(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settlementService.getDriverSettlements(principal.getId()));
    }

    @PostMapping("/settlements")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Request a full or partial settlement from Available balance")
    public ResponseEntity<DriverSettlement> createSettlement(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SettlementRequest request) {
        return new ResponseEntity<>(settlementService.createSettlement(principal.getId(), request.amount()), HttpStatus.CREATED);
    }

    @PostMapping("/settlements/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark a pending settlement as completed")
    public ResponseEntity<DriverSettlement> completeSettlement(
            @PathVariable Long id,
            @RequestParam(required = false) String gatewayReference) {
        return ResponseEntity.ok(settlementService.completeSettlement(id, gatewayReference));
    }

    @PostMapping("/settlements/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark a pending settlement as failed and release reserved funds")
    public ResponseEntity<DriverSettlement> failSettlement(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Bank transfer failed") String reason) {
        return ResponseEntity.ok(settlementService.failSettlement(id, reason));
    }

    @PostMapping("/settlements/{id}/retry")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Safely retry a failed settlement")
    public ResponseEntity<DriverSettlement> retrySettlement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "true") boolean succeed,
            @RequestParam(required = false) String gatewayReference) {
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        return ResponseEntity.ok(settlementService.retrySettlement(id, principal.getId(), isAdmin, succeed, gatewayReference));
    }

    @GetMapping("/settlements/{id}/attempts")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "List attempt history for a settlement")
    public ResponseEntity<List<DriverSettlementAttempt>> getAttempts(@PathVariable Long id) {
        return ResponseEntity.ok(settlementService.getSettlementAttempts(id));
    }

    public record SettlementRequest(BigDecimal amount) {}
}
