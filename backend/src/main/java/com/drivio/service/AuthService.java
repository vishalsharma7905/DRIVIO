package com.drivio.service;

import com.drivio.dto.AuthResponse;
import com.drivio.dto.LoginRequest;
import com.drivio.dto.RefreshTokenRequest;
import com.drivio.dto.RegisterRequest;
import com.drivio.entity.CustomerProfile;
import com.drivio.entity.DriverAvailability;
import com.drivio.entity.DriverProfile;
import com.drivio.entity.User;
import com.drivio.enums.Role;
import com.drivio.enums.VerificationStatus;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.exception.UnauthorizedException;
import com.drivio.repository.CustomerProfileRepository;
import com.drivio.repository.DriverAvailabilityRepository;
import com.drivio.repository.DriverProfileRepository;
import com.drivio.repository.UserRepository;
import com.drivio.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            DriverProfileRepository driverProfileRepository,
            DriverAvailabilityRepository driverAvailabilityRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverAvailabilityRepository = driverAvailabilityRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with email " + request.getEmail() + " already exists");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhoneNumber(),
                request.getRole()
        );
        User savedUser = userRepository.save(user);

        Long profileId = null;
        VerificationStatus verificationStatus = null;

        if (request.getRole() == Role.CUSTOMER) {
            CustomerProfile customerProfile = new CustomerProfile(savedUser);
            CustomerProfile savedProfile = customerProfileRepository.save(customerProfile);
            profileId = savedProfile.getId();
        } else if (request.getRole() == Role.DRIVER) {
            DriverProfile driverProfile = new DriverProfile(savedUser);
            driverProfile.setVerificationStatus(VerificationStatus.PENDING);
            DriverProfile savedProfile = driverProfileRepository.save(driverProfile);

            DriverAvailability availability = new DriverAvailability(savedProfile, false);
            driverAvailabilityRepository.save(availability);

            profileId = savedProfile.getId();
            verificationStatus = VerificationStatus.PENDING;
        }

        String token = tokenProvider.generateTokenFromUser(
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name(), savedUser.getFullName()
        );
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getId());

        return new AuthResponse(
                token, refreshToken, savedUser.getId(), savedUser.getEmail(),
                savedUser.getFullName(), savedUser.getRole(), verificationStatus, profileId
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Your account has been deactivated. Please contact support.");
        }

        Long profileId = null;
        VerificationStatus verificationStatus = null;

        if (user.getRole() == Role.CUSTOMER) {
            CustomerProfile cp = customerProfileRepository.findByUserId(user.getId()).orElse(null);
            if (cp != null) profileId = cp.getId();
        } else if (user.getRole() == Role.DRIVER) {
            DriverProfile dp = driverProfileRepository.findByUserId(user.getId()).orElse(null);
            if (dp != null) {
                profileId = dp.getId();
                verificationStatus = dp.getVerificationStatus();
            }
        }

        String token = tokenProvider.generateTokenFromUser(
                user.getId(), user.getEmail(), user.getRole().name(), user.getFullName()
        );
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(
                token, refreshToken, user.getId(), user.getEmail(),
                user.getFullName(), user.getRole(), verificationStatus, profileId
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long profileId = null;
        VerificationStatus verificationStatus = null;

        if (user.getRole() == Role.CUSTOMER) {
            CustomerProfile cp = customerProfileRepository.findByUserId(user.getId()).orElse(null);
            if (cp != null) profileId = cp.getId();
        } else if (user.getRole() == Role.DRIVER) {
            DriverProfile dp = driverProfileRepository.findByUserId(user.getId()).orElse(null);
            if (dp != null) {
                profileId = dp.getId();
                verificationStatus = dp.getVerificationStatus();
            }
        }

        String newToken = tokenProvider.generateTokenFromUser(
                user.getId(), user.getEmail(), user.getRole().name(), user.getFullName()
        );
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(
                newToken, newRefreshToken, user.getId(), user.getEmail(),
                user.getFullName(), user.getRole(), verificationStatus, profileId
        );
    }
}

