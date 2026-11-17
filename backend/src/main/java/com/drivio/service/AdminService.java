package com.drivio.service;

import com.drivio.dto.*;
import com.drivio.entity.*;
import com.drivio.enums.*;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverDocumentRepository driverDocumentRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final BookingRepository bookingRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;

    public AdminService(
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverDocumentRepository driverDocumentRepository,
            DriverAvailabilityRepository driverAvailabilityRepository,
            BookingRepository bookingRepository,
            ComplaintRepository complaintRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverDocumentRepository = driverDocumentRepository;
        this.driverAvailabilityRepository = driverAvailabilityRepository;
        this.bookingRepository = bookingRepository;
        this.complaintRepository = complaintRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getDashboardStats() {
        AdminDashboardStatsDto stats = new AdminDashboardStatsDto();

        stats.setTotalCustomers(userRepository.countByRole(Role.CUSTOMER));
        stats.setTotalDrivers(driverProfileRepository.count());
        stats.setVerifiedDrivers(driverProfileRepository.countByVerificationStatus(VerificationStatus.VERIFIED));
        stats.setPendingDrivers(driverProfileRepository.countByVerificationStatus(VerificationStatus.PENDING));
        stats.setRejectedDrivers(driverProfileRepository.countByVerificationStatus(VerificationStatus.REJECTED));

        stats.setTotalBookings(bookingRepository.count());
        stats.setActiveBookings(bookingRepository.countActiveBookings());
        stats.setCompletedBookings(bookingRepository.countByBookingStatus(BookingStatus.TRIP_COMPLETED));
        stats.setCancelledBookings(bookingRepository.countByBookingStatus(BookingStatus.CANCELLED));

        BigDecimal revenue = bookingRepository.calculateTotalRevenue();
        stats.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getPhoneNumber(), u.getRole(), u.isActive(), u.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto toggleUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setActive(active);
        User saved = userRepository.save(user);
        return new UserDto(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getPhoneNumber(), saved.getRole(), saved.isActive(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<DriverProfileDto> getDriversByStatus(VerificationStatus status) {
        List<DriverProfile> drivers = (status != null) ?
                driverProfileRepository.findByVerificationStatus(status) :
                driverProfileRepository.findAll();

        return drivers.stream()
                .map(this::mapDriverProfileToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DriverProfileDto verifyDriver(Long adminId, Long driverProfileId, DriverVerificationRequest request) {
        DriverProfile driver = driverProfileRepository.findById(driverProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found with id: " + driverProfileId));

        driver.setVerificationStatus(request.getStatus());
        if (request.getStatus() == VerificationStatus.REJECTED) {
            driver.setRejectionReason(request.getRemarks());
            // If rejected, ensure availability is off
            DriverAvailability da = driverAvailabilityRepository.findByDriverProfileId(driver.getId()).orElse(null);
            if (da != null) {
                da.setAvailable(false);
                da.setStatusNote("KYC Rejected: " + request.getRemarks());
                driverAvailabilityRepository.save(da);
            }
        } else if (request.getStatus() == VerificationStatus.VERIFIED) {
            driver.setRejectionReason(null);
            // Mark documents verified
            List<DriverDocument> documents = driverDocumentRepository.findByDriverProfileId(driver.getId());
            for (DriverDocument doc : documents) {
                doc.setVerificationStatus(VerificationStatus.VERIFIED);
                doc.setVerifiedByAdminId(adminId);
                doc.setRemarks("Approved by Admin");
                driverDocumentRepository.save(doc);
            }
        }

        DriverProfile saved = driverProfileRepository.save(driver);

        // Send notification to driver
        notificationService.sendNotification(
                saved.getUser().getId(),
                "Driver Account " + request.getStatus().name(),
                (request.getStatus() == VerificationStatus.VERIFIED) ?
                        "Congratulations! Your driver account has been verified. You can now toggle your availability and accept trips." :
                        "Your verification request was rejected: " + request.getRemarks(),
                NotificationType.KYC_STATUS_UPDATED,
                saved.getId()
        );

        return mapDriverProfileToDto(saved);
    }

    @Transactional
    public ComplaintDto createComplaint(Long userId, CreateComplaintRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId()).orElse(null);
        }

        Complaint complaint = new Complaint(booking, user, request.getCategory(), request.getDescription());
        Complaint saved = complaintRepository.save(complaint);
        return mapComplaintToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapComplaintToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComplaintDto updateComplaint(Long complaintId, UpdateComplaintRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        complaint.setStatus(request.getStatus());
        if (request.getAdminNotes() != null) {
            complaint.setAdminNotes(request.getAdminNotes());
        }

        Complaint saved = complaintRepository.save(complaint);
        return mapComplaintToDto(saved);
    }

    private DriverProfileDto mapDriverProfileToDto(DriverProfile profile) {
        DriverProfileDto dto = new DriverProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUser().getId());
        dto.setEmail(profile.getUser().getEmail());
        dto.setFullName(profile.getUser().getFullName());
        dto.setPhoneNumber(profile.getUser().getPhoneNumber());
        dto.setProfilePhotoUrl(profile.getProfilePhotoUrl());
        dto.setLicenceNumber(profile.getLicenceNumber());
        dto.setLicenceExpiryDate(profile.getLicenceExpiryDate());
        dto.setLicenceDocumentUrl(profile.getLicenceDocumentUrl());
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setLanguages(profile.getLanguages());
        dto.setVehicleCategories(profile.getVehicleCategories());
        dto.setServicePreferences(profile.getServicePreferences());
        dto.setVerificationStatus(profile.getVerificationStatus());
        dto.setRejectionReason(profile.getRejectionReason());
        dto.setRating(profile.getRating());
        dto.setTotalRatingsCount(profile.getTotalRatingsCount());
        dto.setTotalTrips(profile.getTotalTrips());
        dto.setTotalEarnings(profile.getTotalEarnings());
        dto.setCurrentLatitude(profile.getCurrentLatitude());
        dto.setCurrentLongitude(profile.getCurrentLongitude());
        dto.setLastLocationUpdate(profile.getLastLocationUpdate());
        dto.setCreatedAt(profile.getCreatedAt());

        DriverAvailability da = driverAvailabilityRepository.findByDriverProfileId(profile.getId()).orElse(null);
        if (da != null) {
            dto.setAvailable(da.isAvailable());
            dto.setStatusNote(da.getStatusNote());
        } else {
            dto.setAvailable(false);
        }

        return dto;
    }

    private ComplaintDto mapComplaintToDto(Complaint complaint) {
        ComplaintDto dto = new ComplaintDto();
        dto.setId(complaint.getId());
        if (complaint.getBooking() != null) {
            dto.setBookingId(complaint.getBooking().getId());
            dto.setBookingReference(complaint.getBooking().getBookingReference());
        }
        dto.setUserId(complaint.getUser().getId());
        dto.setUserName(complaint.getUser().getFullName());
        dto.setUserEmail(complaint.getUser().getEmail());
        dto.setCategory(complaint.getCategory());
        dto.setDescription(complaint.getDescription());
        dto.setStatus(complaint.getStatus());
        dto.setAdminNotes(complaint.getAdminNotes());
        dto.setCreatedAt(complaint.getCreatedAt());
        dto.setUpdatedAt(complaint.getUpdatedAt());
        return dto;
    }
}

