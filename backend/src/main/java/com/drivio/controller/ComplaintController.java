package com.drivio.controller;

import com.drivio.dto.ComplaintDto;
import com.drivio.dto.CreateComplaintRequest;
import com.drivio.security.UserPrincipal;
import com.drivio.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "Complaints", description = "User support and complaint submission")
public class ComplaintController {

    private final AdminService adminService;

    public ComplaintController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    @Operation(summary = "Submit a complaint regarding a booking or platform issue")
    public ResponseEntity<ComplaintDto> createComplaint(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateComplaintRequest request) {
        return new ResponseEntity<>(adminService.createComplaint(userPrincipal.getId(), request), HttpStatus.CREATED);
    }
}

