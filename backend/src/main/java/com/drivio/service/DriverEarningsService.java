package com.drivio.service;

import com.drivio.entity.DriverEarnings;
import com.drivio.entity.DriverProfile;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.DriverEarningsRepository;
import com.drivio.repository.DriverProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DriverEarningsService {

    private final DriverEarningsRepository earningsRepository;
    private final DriverProfileRepository driverProfileRepository;

    public DriverEarningsService(
            DriverEarningsRepository earningsRepository,
            DriverProfileRepository driverProfileRepository) {
        this.earningsRepository = earningsRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    @Transactional(readOnly = true)
    public DriverBalanceSummary getBalances(Long driverUserId) {
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return getBalancesByDriverProfileId(driver.getId());
    }

    @Transactional(readOnly = true)
    public DriverBalanceSummary getBalancesByDriverProfileId(Long driverProfileId) {
        List<DriverEarnings> all = earningsRepository.findByDriverProfileIdOrderByAccruedAtDesc(driverProfileId);
        BigDecimal accrued = sumByStatus(all, "ACCRUED");
        BigDecimal available = sumByStatus(all, "AVAILABLE");
        BigDecimal reserved = sumByStatus(all, "RESERVED");
        BigDecimal settled = sumByStatus(all, "SETTLED");
        BigDecimal adjustments = sumByStatus(all, "ADJUSTED");

        // Negative adjustments reduce available first (without touching settled)
        BigDecimal netAvailable = available.add(adjustments).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new DriverBalanceSummary(
                driverProfileId,
                accrued.setScale(2, RoundingMode.HALF_UP),
                netAvailable,
                reserved.setScale(2, RoundingMode.HALF_UP),
                settled.setScale(2, RoundingMode.HALF_UP),
                all
        );
    }

    @Transactional
    public void releaseAccruedToAvailableForBooking(Long bookingId) {
        List<DriverEarnings> rows = earningsRepository.findByBookingId(bookingId);
        for (DriverEarnings e : rows) {
            if ("ACCRUED".equals(e.getStatus())) {
                e.setStatus("AVAILABLE");
                earningsRepository.save(e);
            }
        }
    }

    private BigDecimal sumByStatus(List<DriverEarnings> list, String status) {
        return list.stream()
                .filter(e -> status.equals(e.getStatus()))
                .map(DriverEarnings::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record DriverBalanceSummary(
            Long driverProfileId,
            BigDecimal accrued,
            BigDecimal available,
            BigDecimal reserved,
            BigDecimal settled,
            List<DriverEarnings> history
    ) {}
}
