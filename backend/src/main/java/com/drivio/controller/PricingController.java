package com.drivio.controller;

import com.drivio.dto.FareEstimateRequest;
import com.drivio.dto.FareEstimateResponse;
import com.drivio.dto.PricingConfigurationDto;
import com.drivio.dto.VehiclePricingConfigurationDto;
import com.drivio.entity.PricingAuditLog;
import com.drivio.security.UserPrincipal;
import com.drivio.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing")
@Tag(name = "Pricing", description = "Fare estimation and pricing configuration")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/estimate")
    @Operation(summary = "Calculate transparent estimated fare breakdown for trip")
    public ResponseEntity<FareEstimateResponse> getEstimate(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(pricingService.calculateEstimatedFare(request));
    }

    @GetMapping("/configurations")
    @Operation(summary = "List all active pricing configurations for all services")
    public ResponseEntity<List<PricingConfigurationDto>> getConfigurations() {
        return ResponseEntity.ok(pricingService.getAllPricingConfigurations());
    }

    @PutMapping("/configurations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update pricing rules (Admin only)")
    public ResponseEntity<PricingConfigurationDto> updateConfiguration(
            @PathVariable Long id,
            @Valid @RequestBody PricingConfigurationDto dto) {
        return ResponseEntity.ok(pricingService.updatePricingConfiguration(id, dto));
    }

    @GetMapping("/vehicles")
    @Operation(summary = "List Delhi NCR normal ride vehicle category pricing configurations")
    public ResponseEntity<List<VehiclePricingConfigurationDto>> getVehicleConfigurations() {
        return ResponseEntity.ok(pricingService.getAllVehiclePricingConfigurations());
    }

    @PostMapping("/vehicles/estimates")
    @Operation(summary = "Calculate fare estimates across all 6 normal ride vehicle categories")
    public ResponseEntity<List<FareEstimateResponse>> getAllVehicleEstimates(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(pricingService.getAllNormalRideEstimates(request));
    }

    @PutMapping("/vehicles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update normal ride vehicle pricing rules with audit log (Admin only)")
    public ResponseEntity<VehiclePricingConfigurationDto> updateVehicleConfiguration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal adminUser,
            @Valid @RequestBody VehiclePricingConfigurationDto dto) {
        return ResponseEntity.ok(pricingService.updateVehiclePricingConfiguration(id, dto, adminUser.getId()));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pricing configuration change audit trail (Admin only)")
    public ResponseEntity<List<PricingAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(pricingService.getPricingAuditLogs());
    }
}

