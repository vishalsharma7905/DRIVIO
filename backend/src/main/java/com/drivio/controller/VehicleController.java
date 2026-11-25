package com.drivio.controller;

import com.drivio.entity.User;
import com.drivio.entity.Vehicle;
import com.drivio.enums.VehicleType;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.exception.UnauthorizedException;
import com.drivio.repository.UserRepository;
import com.drivio.repository.VehicleRepository;
import com.drivio.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Customer personal vehicle management for Own-Car Driver and Car Delivery services")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleController(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "List vehicles owned by current customer")
    public ResponseEntity<List<Vehicle>> getMyVehicles(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(vehicleRepository.findByUserId(userPrincipal.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Register a personal vehicle")
    public ResponseEntity<Vehicle> addVehicle(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateVehicleRequest request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Vehicle v = new Vehicle();
        v.setUser(user);
        v.setVehicleType(request.vehicleType());
        v.setMake(request.make());
        v.setModel(request.model());
        v.setYear(request.year());
        v.setRegistrationNumber(request.registrationNumber().toUpperCase());
        v.setTransmission(request.transmission() != null ? request.transmission() : "MANUAL");
        return new ResponseEntity<>(vehicleRepository.save(v), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Delete a personal vehicle")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
        if (!v.getUser().getId().equals(userPrincipal.getId())) {
            throw new UnauthorizedException("You can only delete your own vehicles");
        }
        vehicleRepository.delete(v);
        return ResponseEntity.noContent().build();
    }

    public record CreateVehicleRequest(
            @NotNull VehicleType vehicleType,
            @NotBlank String make,
            @NotBlank String model,
            Integer year,
            @NotBlank String registrationNumber,
            String transmission
    ) {}
}
