package com.drivio.controller;

import com.drivio.entity.Booking;
import com.drivio.entity.BookingQuotation;
import com.drivio.entity.DriverProfile;
import com.drivio.enums.NotificationType;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.exception.UnauthorizedException;
import com.drivio.repository.BookingQuotationRepository;
import com.drivio.repository.BookingRepository;
import com.drivio.repository.DriverProfileRepository;
import com.drivio.security.UserPrincipal;
import com.drivio.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings/{bookingId}/quotations")
@Tag(name = "Quotations & Negotiation", description = "Driver fare quotations and customer counter-offer negotiation")
public class QuotationController {

    private final BookingQuotationRepository quotationRepository;
    private final BookingRepository bookingRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final NotificationService notificationService;

    public QuotationController(
            BookingQuotationRepository quotationRepository,
            BookingRepository bookingRepository,
            DriverProfileRepository driverProfileRepository,
            NotificationService notificationService) {
        this.quotationRepository = quotationRepository;
        this.bookingRepository = bookingRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List fare quotations for a booking")
    public ResponseEntity<List<QuotationDto>> listQuotations(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        boolean isCustomer = booking.getCustomer().getId().equals(userPrincipal.getId());
        boolean isDriver = booking.getDriver() != null && booking.getDriver().getUser().getId().equals(userPrincipal.getId());
        if (!isCustomer && !isDriver && userPrincipal.getRole().name().equals("ADMIN") == false) {
            throw new UnauthorizedException("Not authorized to view quotations for this booking");
        }
        List<QuotationDto> dtos = quotationRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Transactional
    @Operation(summary = "Driver proposes a fare quotation for a booking")
    public ResponseEntity<QuotationDto> proposeQuotation(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProposeQuotationRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        DriverProfile driver = driverProfileRepository.findByUserId(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        BookingQuotation q = new BookingQuotation();
        q.setBooking(booking);
        q.setDriver(driver);
        q.setProposedFare(request.proposedFare());
        q.setNotes(request.notes());
        q.setStatus("PENDING");
        BookingQuotation saved = quotationRepository.save(q);

        notificationService.sendNotification(
                booking.getCustomer().getId(),
                "New Driver Quote (₹" + request.proposedFare() + ")",
                driver.getUser().getFullName() + " proposed ₹" + request.proposedFare() + " for booking " + booking.getBookingReference(),
                NotificationType.SYSTEM_ALERT,
                booking.getId()
        );

        return new ResponseEntity<>(mapToDto(saved), HttpStatus.CREATED);
    }

    @PostMapping("/{quotationId}/counter")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Transactional
    @Operation(summary = "Customer submits a counter-offer fare")
    public ResponseEntity<QuotationDto> counterQuotation(
            @PathVariable Long bookingId,
            @PathVariable Long quotationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CounterQuotationRequest request) {
        BookingQuotation q = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found: " + quotationId));
        if (!q.getBooking().getId().equals(bookingId) || !q.getBooking().getCustomer().getId().equals(userPrincipal.getId())) {
            throw new UnauthorizedException("Not authorized to counter this quotation");
        }
        q.setCustomerCounterFare(request.counterFare());
        q.setStatus("COUNTERED");
        if (request.notes() != null) q.setNotes(request.notes());
        BookingQuotation saved = quotationRepository.save(q);

        notificationService.sendNotification(
                q.getDriver().getUser().getId(),
                "Customer Counter Offer (₹" + request.counterFare() + ")",
                "Customer countered with ₹" + request.counterFare() + " for booking " + q.getBooking().getBookingReference(),
                NotificationType.SYSTEM_ALERT,
                bookingId
        );
        return ResponseEntity.ok(mapToDto(saved));
    }

    @PostMapping("/{quotationId}/accept")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Transactional
    @Operation(summary = "Customer accepts a quotation and updates booking fare")
    public ResponseEntity<QuotationDto> acceptQuotation(
            @PathVariable Long bookingId,
            @PathVariable Long quotationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookingQuotation q = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found: " + quotationId));
        Booking booking = q.getBooking();
        if (!booking.getId().equals(bookingId) || !booking.getCustomer().getId().equals(userPrincipal.getId())) {
            throw new UnauthorizedException("Not authorized to accept this quotation");
        }
        if ("REJECTED".equals(q.getStatus())) {
            throw new BadRequestException("Cannot accept a rejected quotation");
        }
        BigDecimal agreedFare = q.getCustomerCounterFare() != null ? q.getCustomerCounterFare() : q.getProposedFare();
        q.setStatus("ACCEPTED");
        booking.setTotalFare(agreedFare);
        booking.setDriver(q.getDriver());
        bookingRepository.save(booking);
        BookingQuotation saved = quotationRepository.save(q);

        notificationService.sendNotification(
                q.getDriver().getUser().getId(),
                "Quotation Accepted (₹" + agreedFare + ")",
                "Your quote of ₹" + agreedFare + " was accepted for booking " + booking.getBookingReference(),
                NotificationType.DRIVER_ASSIGNED,
                booking.getId()
        );
        return ResponseEntity.ok(mapToDto(saved));
    }

    private QuotationDto mapToDto(BookingQuotation q) {
        return new QuotationDto(
                q.getId(),
                q.getBooking().getId(),
                q.getDriver().getId(),
                q.getDriver().getUser().getFullName(),
                q.getProposedFare(),
                q.getCustomerCounterFare(),
                q.getStatus(),
                q.getNotes(),
                q.getCreatedAt()
        );
    }

    public record ProposeQuotationRequest(@NotNull @DecimalMin("1.0") BigDecimal proposedFare, String notes) {}
    public record CounterQuotationRequest(@NotNull @DecimalMin("1.0") BigDecimal counterFare, String notes) {}
    public record QuotationDto(
            Long id, Long bookingId, Long driverId, String driverName,
            BigDecimal proposedFare, BigDecimal customerCounterFare,
            String status, String notes, LocalDateTime createdAt
    ) {}
}
