package com.drivio.controller;

import com.drivio.dto.DriverRecommendationDto;
import com.drivio.dto.RecommendationRequest;
import com.drivio.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers/recommendations")
@Tag(name = "Driver Recommendations", description = "Rule-based multi-criteria driver matching algorithm")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    @Operation(summary = "Find suitable available drivers ranked by weighted recommendation score")
    public ResponseEntity<List<DriverRecommendationDto>> getRecommendations(@Valid @RequestBody RecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.recommendDrivers(request));
    }
}

