package com.drivio.controller;

import com.drivio.dto.FareEstimateRequest;
import com.drivio.dto.FareEstimateResponse;
import com.drivio.dto.PricingConfigurationDto;
import com.drivio.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
}

