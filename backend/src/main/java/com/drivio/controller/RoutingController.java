package com.drivio.controller;

import com.drivio.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routing")
@Tag(name = "Routing", description = "OSRM road-snapped route distance, ETA, polyline geometry, and Nominatim geocoding")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/route")
    @Operation(summary = "Calculate road-snapped route between pickup and destination")
    public ResponseEntity<RoutingService.RouteResult> getRoute(
            @RequestParam double pickupLat,
            @RequestParam double pickupLng,
            @RequestParam double destLat,
            @RequestParam double destLng) {
        return ResponseEntity.ok(routingService.calculateRoute(pickupLat, pickupLng, destLat, destLng));
    }

    @GetMapping("/geocode")
    @Operation(summary = "Search addresses using OpenStreetMap Nominatim")
    public ResponseEntity<List<RoutingService.GeocodeSuggestion>> geocode(@RequestParam String q) {
        return ResponseEntity.ok(routingService.searchAddress(q));
    }

    @GetMapping("/reverse")
    @Operation(summary = "Reverse geocode coordinates using OpenStreetMap Nominatim")
    public ResponseEntity<RoutingService.GeocodeSuggestion> reverse(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(routingService.reverseGeocode(lat, lng));
    }
}
