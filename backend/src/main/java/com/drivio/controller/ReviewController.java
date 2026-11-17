package com.drivio.controller;

import com.drivio.dto.CreateReviewRequest;
import com.drivio.dto.ReviewDto;
import com.drivio.security.UserPrincipal;
import com.drivio.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Driver ratings and trip feedback")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @Operation(summary = "Submit driver rating (1-5) and review for a completed trip")
    public ResponseEntity<ReviewDto> createReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateReviewRequest request) {
        return new ResponseEntity<>(reviewService.createReview(userPrincipal.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get reviews for a driver")
    public ResponseEntity<List<ReviewDto>> getDriverReviews(@PathVariable Long driverId) {
        return ResponseEntity.ok(reviewService.getDriverReviews(driverId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all platform reviews (Admin only)")
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }
}

