package com.drivio.service;

import com.drivio.dto.DriverAvailabilityDto;
import com.drivio.dto.DriverDocumentDto;
import com.drivio.dto.DriverProfileDto;
import com.drivio.dto.UpdateAvailabilityRequest;
import com.drivio.dto.UpdateDriverProfileRequest;
import com.drivio.entity.DriverAvailability;
import com.drivio.entity.DriverDocument;
import com.drivio.entity.DriverProfile;
import com.drivio.entity.User;
import com.drivio.enums.Role;
import com.drivio.enums.VerificationStatus;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.exception.UnauthorizedException;
import com.drivio.repository.DriverAvailabilityRepository;
import com.drivio.repository.DriverDocumentRepository;
import com.drivio.repository.DriverProfileRepository;
import com.drivio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final DriverDocumentRepository driverDocumentRepository;
    private final UserRepository userRepository;

    @Value("${drivio.storage.upload-dir:./uploads}")
    private String uploadDir;

    public DriverService(
            DriverProfileRepository driverProfileRepository,
            DriverAvailabilityRepository driverAvailabilityRepository,
            DriverDocumentRepository driverDocumentRepository,
            UserRepository userRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.driverAvailabilityRepository = driverAvailabilityRepository;
        this.driverDocumentRepository = driverDocumentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DriverProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    DriverProfile newProfile = new DriverProfile(user);
                    DriverProfile saved = driverProfileRepository.save(newProfile);
                    DriverAvailability da = new DriverAvailability(saved, false);
                    driverAvailabilityRepository.save(da);
                    return saved;
                });

        return mapToDto(user, profile);
    }

    @Transactional
    public DriverProfileDto updateProfile(Long userId, UpdateDriverProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        userRepository.save(user);

        if (request.getProfilePhotoUrl() != null) profile.setProfilePhotoUrl(request.getProfilePhotoUrl());
        if (request.getLicenceNumber() != null) profile.setLicenceNumber(request.getLicenceNumber());
        if (request.getLicenceExpiryDate() != null) profile.setLicenceExpiryDate(request.getLicenceExpiryDate());
        if (request.getLicenceDocumentUrl() != null) profile.setLicenceDocumentUrl(request.getLicenceDocumentUrl());
        if (request.getExperienceYears() != null) profile.setExperienceYears(request.getExperienceYears());
        if (request.getLanguages() != null) profile.setLanguages(request.getLanguages());
        if (request.getVehicleCategories() != null) profile.setVehicleCategories(request.getVehicleCategories());
        if (request.getServicePreferences() != null) profile.setServicePreferences(request.getServicePreferences());

        DriverProfile updated = driverProfileRepository.save(profile);
        return mapToDto(user, updated);
    }

    @Transactional
    public DriverAvailabilityDto updateAvailability(Long userId, UpdateAvailabilityRequest request) {
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        // Only verified drivers can toggle availability to true
        if (request.isAvailable() && profile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BadRequestException("Your driver profile is currently " + profile.getVerificationStatus() +
                    ". Only VERIFIED drivers can set their status to available.");
        }

        DriverAvailability availability = driverAvailabilityRepository.findByDriverProfileId(profile.getId())
                .orElseGet(() -> new DriverAvailability(profile, false));

        availability.setAvailable(request.isAvailable());
        if (request.getShiftStart() != null) availability.setShiftStart(request.getShiftStart());
        if (request.getShiftEnd() != null) availability.setShiftEnd(request.getShiftEnd());
        if (request.getStatusNote() != null) availability.setStatusNote(request.getStatusNote());
        availability.setUpdatedAt(LocalDateTime.now());

        DriverAvailability saved = driverAvailabilityRepository.save(availability);

        DriverAvailabilityDto dto = new DriverAvailabilityDto();
        dto.setId(saved.getId());
        dto.setDriverId(profile.getId());
        dto.setAvailable(saved.isAvailable());
        dto.setShiftStart(saved.getShiftStart());
        dto.setShiftEnd(saved.getShiftEnd());
        dto.setStatusNote(saved.getStatusNote());
        return dto;
    }

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".png", ".jpg", ".jpeg", ".webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf", "image/png", "image/jpeg", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Transactional
    public DriverDocumentDto uploadDocument(Long userId, String documentType, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum permitted limit of 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file extension. Only PDF, PNG, JPG, JPEG, and WEBP files are permitted.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file content type (" + contentType + "). Only PDF and images are accepted.");
        }

        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID() + extension;
        Path targetPath = Paths.get(uploadDir).resolve(fileName).normalize();

        // Verify no path traversal outside uploadDir
        if (!targetPath.startsWith(Paths.get(uploadDir).normalize())) {
            throw new BadRequestException("Security violation: Invalid upload file path");
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/api/drivers/documents/file/" + fileName;

        // If driving license, also update licence document url on profile
        if ("DRIVING_LICENCE".equalsIgnoreCase(documentType)) {
            profile.setLicenceDocumentUrl(fileUrl);
            driverProfileRepository.save(profile);
        } else if ("PROFILE_PHOTO".equalsIgnoreCase(documentType)) {
            profile.setProfilePhotoUrl(fileUrl);
            driverProfileRepository.save(profile);
        }

        DriverDocument document = new DriverDocument(profile, documentType.toUpperCase(), fileUrl);
        DriverDocument saved = driverDocumentRepository.save(document);

        DriverDocumentDto dto = new DriverDocumentDto();
        dto.setId(saved.getId());
        dto.setDriverId(profile.getId());
        dto.setDocumentType(saved.getDocumentType());
        dto.setDocumentUrl(saved.getDocumentUrl());
        dto.setVerificationStatus(saved.getVerificationStatus());
        dto.setRemarks(saved.getRemarks());
        dto.setCreatedAt(saved.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getDocumentFile(Long requestingUserId, Role requestingRole, Long documentId) throws IOException {
        DriverDocument document = driverDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        boolean isAdmin = (requestingRole == Role.ADMIN);
        boolean isOwnerDriver = document.getDriverProfile().getUser().getId().equals(requestingUserId);

        if (!isAdmin && !isOwnerDriver) {
            throw new UnauthorizedException("Access denied: You are not authorized to view this document");
        }

        String docUrl = document.getDocumentUrl();
        String fileName = docUrl.substring(docUrl.lastIndexOf("/") + 1);
        Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();

        if (!filePath.startsWith(Paths.get(uploadDir).normalize()) || !Files.exists(filePath)) {
            throw new ResourceNotFoundException("Document file not found on disk");
        }

        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @Transactional(readOnly = true)
    public List<DriverDocumentDto> getDriverDocuments(Long userId) {
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        return driverDocumentRepository.findByDriverProfileId(profile.getId()).stream()
                .map(doc -> {
                    DriverDocumentDto dto = new DriverDocumentDto();
                    dto.setId(doc.getId());
                    dto.setDriverId(profile.getId());
                    dto.setDocumentType(doc.getDocumentType());
                    dto.setDocumentUrl(doc.getDocumentUrl());
                    dto.setVerificationStatus(doc.getVerificationStatus());
                    dto.setRemarks(doc.getRemarks());
                    dto.setVerifiedByAdminId(doc.getVerifiedByAdminId());
                    dto.setCreatedAt(doc.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private DriverProfileDto mapToDto(User user, DriverProfile profile) {
        DriverProfileDto dto = new DriverProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
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
}

