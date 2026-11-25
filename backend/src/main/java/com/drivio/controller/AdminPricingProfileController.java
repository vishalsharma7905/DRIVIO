package com.drivio.controller;

import com.drivio.entity.PricingAuditLogV2;
import com.drivio.entity.PricingProfile;
import com.drivio.entity.PricingProfileVersion;
import com.drivio.security.UserPrincipal;
import com.drivio.service.PricingProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Pricing Profiles", description = "Unified versioned pricing profile management and audit trail")
public class AdminPricingProfileController {

    private final PricingProfileService pricingProfileService;

    public AdminPricingProfileController(PricingProfileService pricingProfileService) {
        this.pricingProfileService = pricingProfileService;
    }

    @GetMapping("/profiles")
    @Operation(summary = "List all unified pricing profiles")
    public ResponseEntity<List<PricingProfile>> listProfiles() {
        return ResponseEntity.ok(pricingProfileService.listProfiles());
    }

    @PostMapping("/profiles")
    @Operation(summary = "Create a new pricing profile")
    public ResponseEntity<PricingProfile> createProfile(
            @RequestBody PricingProfile profile,
            @AuthenticationPrincipal UserPrincipal admin) {
        return new ResponseEntity<>(pricingProfileService.createProfile(profile, admin.getId()), HttpStatus.CREATED);
    }

    @GetMapping("/versions")
    @Operation(summary = "List all pricing profile versions (active, upcoming, draft, retired)")
    public ResponseEntity<List<PricingProfileVersion>> listVersions(@RequestParam(required = false) String filter) {
        if ("active".equalsIgnoreCase(filter)) return ResponseEntity.ok(pricingProfileService.getActiveVersions());
        if ("upcoming".equalsIgnoreCase(filter)) return ResponseEntity.ok(pricingProfileService.getUpcomingVersions());
        if ("expired".equalsIgnoreCase(filter)) return ResponseEntity.ok(pricingProfileService.getExpiredOrRetiredVersions());
        return ResponseEntity.ok(pricingProfileService.getAllVersions());
    }

    @PostMapping("/profiles/{profileId}/versions")
    @Operation(summary = "Create a new DRAFT pricing version")
    public ResponseEntity<PricingProfileVersion> createVersion(
            @PathVariable Long profileId,
            @RequestBody PricingProfileVersion version,
            @AuthenticationPrincipal UserPrincipal admin) {
        return new ResponseEntity<>(pricingProfileService.createVersion(profileId, version, admin.getId()), HttpStatus.CREATED);
    }

    @PutMapping("/versions/{versionId}")
    @Operation(summary = "Update an unpublished DRAFT pricing version")
    public ResponseEntity<PricingProfileVersion> updateDraftVersion(
            @PathVariable Long versionId,
            @RequestBody PricingProfileVersion updates,
            @AuthenticationPrincipal UserPrincipal admin) {
        return ResponseEntity.ok(pricingProfileService.updateDraftVersion(versionId, updates, admin.getId()));
    }

    @PostMapping("/versions/{versionId}/publish")
    @Operation(summary = "Publish or schedule a DRAFT pricing version")
    public ResponseEntity<PricingProfileVersion> publishVersion(
            @PathVariable Long versionId,
            @RequestParam(required = false) OffsetDateTime effectiveFrom,
            @AuthenticationPrincipal UserPrincipal admin) {
        return ResponseEntity.ok(pricingProfileService.publishVersion(versionId, effectiveFrom, admin.getId()));
    }

    @PostMapping("/versions/{versionId}/retire")
    @Operation(summary = "Retire/deactivate a pricing version")
    public ResponseEntity<PricingProfileVersion> retireVersion(
            @PathVariable Long versionId,
            @AuthenticationPrincipal UserPrincipal admin) {
        return ResponseEntity.ok(pricingProfileService.retireVersion(versionId, admin.getId()));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View unified pricing audit history")
    public ResponseEntity<List<PricingAuditLogV2>> getAuditLogs() {
        return ResponseEntity.ok(pricingProfileService.getAuditLogsV2());
    }
}
