package com.drivio.service;

import com.drivio.dto.CustomerProfileDto;
import com.drivio.dto.UpdateCustomerProfileRequest;
import com.drivio.entity.CustomerProfile;
import com.drivio.entity.User;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.CustomerProfileRepository;
import com.drivio.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerProfileRepository customerProfileRepository, UserRepository userRepository) {
        this.customerProfileRepository = customerProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> customerProfileRepository.save(new CustomerProfile(user)));

        return mapToDto(user, profile);
    }

    @Transactional
    public CustomerProfileDto updateProfile(Long userId, UpdateCustomerProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> new CustomerProfile(user));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        userRepository.save(user);

        if (request.getPreferredLanguage() != null) {
            profile.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getEmergencyContact() != null) {
            profile.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getHomeAddress() != null) {
            profile.setHomeAddress(request.getHomeAddress());
        }

        CustomerProfile updated = customerProfileRepository.save(profile);
        return mapToDto(user, updated);
    }

    private CustomerProfileDto mapToDto(User user, CustomerProfile profile) {
        CustomerProfileDto dto = new CustomerProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPreferredLanguage(profile.getPreferredLanguage());
        dto.setEmergencyContact(profile.getEmergencyContact());
        dto.setHomeAddress(profile.getHomeAddress());
        dto.setCreatedAt(profile.getCreatedAt());
        return dto;
    }
}

