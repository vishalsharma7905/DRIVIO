package com.drivio.service;

import com.drivio.entity.*;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.exception.UnauthorizedException;
import com.drivio.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementService {

    private final DriverSettlementsRepository settlementsRepository;
    private final DriverSettlementItemsRepository itemsRepository;
    private final DriverSettlementAttemptsRepository attemptsRepository;
    private final DriverEarningsRepository earningsRepository;
    private final DriverProfileRepository driverProfileRepository;

    public SettlementService(
            DriverSettlementsRepository settlementsRepository,
            DriverSettlementItemsRepository itemsRepository,
            DriverSettlementAttemptsRepository attemptsRepository,
            DriverEarningsRepository earningsRepository,
            DriverProfileRepository driverProfileRepository) {
        this.settlementsRepository = settlementsRepository;
        this.itemsRepository = itemsRepository;
        this.attemptsRepository = attemptsRepository;
        this.earningsRepository = earningsRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    @Transactional
    public DriverSettlement createSettlement(Long driverUserId, BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Requested settlement amount must be positive");
        }
        BigDecimal target = requestedAmount.setScale(2, RoundingMode.HALF_UP);
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        List<DriverEarnings> availableRows = earningsRepository.findByDriverProfileIdOrderByAccruedAtDesc(driver.getId())
                .stream()
                .filter(e -> "AVAILABLE".equals(e.getStatus()) && e.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal totalAvailable = availableRows.stream()
                .map(DriverEarnings::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAvailable.compareTo(target) < 0) {
            throw new BadRequestException("Insufficient available balance. Available: ₹" + totalAvailable + ", Requested: ₹" + target);
        }

        DriverSettlement settlement = new DriverSettlement();
        settlement.setDriverProfileId(driver.getId());
        settlement.setTotalAmount(target);
        settlement.setStatus("PENDING");
        settlement.setInitiatedAt(LocalDateTime.now());
        DriverSettlement savedSettlement = settlementsRepository.save(settlement);

        reserveAvailableEarnings(driver.getId(), savedSettlement.getId(), target, availableRows);

        DriverSettlementAttempt attempt = new DriverSettlementAttempt();
        attempt.setSettlementId(savedSettlement.getId());
        attempt.setAttemptNumber(1);
        attempt.setStatus("PENDING");
        attempt.setTriedAt(LocalDateTime.now());
        attemptsRepository.save(attempt);

        return savedSettlement;
    }

    private void reserveAvailableEarnings(Long driverProfileId, Long settlementId, BigDecimal target, List<DriverEarnings> availableRows) {
        BigDecimal remaining = target;
        for (DriverEarnings earning : availableRows) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal amt = earning.getAmount();
            if (amt.compareTo(remaining) <= 0) {
                earning.setStatus("RESERVED");
                earningsRepository.save(earning);
                createSettlementItem(settlementId, earning.getId(), earning.getBookingId(), amt);
                remaining = remaining.subtract(amt);
            } else {
                // Split earning row for exact partial settlement:
                // 1) Reduce original row to remaining AVAILABLE balance
                // 2) Create RESERVED row for the exact reserved portion
                BigDecimal leftover = amt.subtract(remaining);
                earning.setAmount(leftover);
                earningsRepository.save(earning);

                DriverEarnings reservedPortion = new DriverEarnings();
                reservedPortion.setDriverProfileId(driverProfileId);
                reservedPortion.setBookingId(null); // Avoid unique index collision on booking_id
                reservedPortion.setAmount(remaining);
                reservedPortion.setStatus("RESERVED");
                reservedPortion.setDescription("Reserved split from earning #" + earning.getId() + " for settlement #" + settlementId);
                DriverEarnings savedReserved = earningsRepository.save(reservedPortion);

                createSettlementItem(settlementId, savedReserved.getId(), earning.getBookingId(), remaining);
                remaining = BigDecimal.ZERO;
            }
        }
    }

    private void createSettlementItem(Long settlementId, Long earningsId, Long bookingId, BigDecimal amount) {
        DriverSettlementItem item = new DriverSettlementItem();
        item.setSettlementId(settlementId);
        item.setDriverEarningsId(earningsId);
        item.setBookingId(bookingId);
        item.setAmount(amount);
        item.setStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        itemsRepository.save(item);
    }

    @Transactional
    public DriverSettlement completeSettlement(Long settlementId, String gatewayReference) {
        DriverSettlement settlement = settlementsRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));
        if ("COMPLETED".equals(settlement.getStatus())) {
            throw new BadRequestException("Settlement is already completed");
        }
        if ("FAILED".equals(settlement.getStatus())) {
            throw new BadRequestException("Cannot complete a FAILED settlement directly; use retry instead");
        }

        List<DriverSettlementItem> items = itemsRepository.findBySettlementIdOrderByCreatedAtDesc(settlementId);
        LocalDateTime now = LocalDateTime.now();
        for (DriverSettlementItem item : items) {
            item.setStatus("COMPLETED");
            item.setSettledAt(now);
            itemsRepository.save(item);

            if (item.getDriverEarningsId() != null) {
                earningsRepository.findById(item.getDriverEarningsId()).ifPresent(e -> {
                    e.setStatus("SETTLED");
                    e.setSettledAt(now);
                    earningsRepository.save(e);
                });
            }
        }

        settlement.setStatus("COMPLETED");
        settlement.setCompletedAt(now);
        DriverSettlement saved = settlementsRepository.save(settlement);

        // Update latest pending attempt to SUCCESS
        List<DriverSettlementAttempt> attempts = attemptsRepository.findBySettlementIdOrderByTriedAtDesc(settlementId);
        if (!attempts.isEmpty()) {
            DriverSettlementAttempt latest = attempts.get(0);
            latest.setStatus("SUCCESS");
            latest.setCompletedAt(now);
            latest.setGatewayReference(gatewayReference != null ? gatewayReference : "STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            attemptsRepository.save(latest);
        }

        // Sync DriverProfile.totalEarnings as sum of SETTLED earnings
        DriverProfile driver = driverProfileRepository.findById(settlement.getDriverProfileId()).orElse(null);
        if (driver != null) {
            BigDecimal settledSum = earningsRepository.findByDriverProfileIdOrderByAccruedAtDesc(driver.getId()).stream()
                    .filter(e -> "SETTLED".equals(e.getStatus()))
                    .map(DriverEarnings::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            driver.setTotalEarnings(settledSum);
            driverProfileRepository.save(driver);
        }

        return saved;
    }

    @Transactional
    public DriverSettlement failSettlement(Long settlementId, String errorMessage) {
        DriverSettlement settlement = settlementsRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));
        if ("COMPLETED".equals(settlement.getStatus())) {
            throw new BadRequestException("Cannot fail an already COMPLETED settlement");
        }

        LocalDateTime now = LocalDateTime.now();
        List<DriverSettlementItem> items = itemsRepository.findBySettlementIdOrderByCreatedAtDesc(settlementId);
        for (DriverSettlementItem item : items) {
            if ("PENDING".equals(item.getStatus())) {
                item.setStatus("FAILED");
                itemsRepository.save(item);

                // Release RESERVED earnings back to AVAILABLE
                if (item.getDriverEarningsId() != null) {
                    earningsRepository.findById(item.getDriverEarningsId()).ifPresent(e -> {
                        if ("RESERVED".equals(e.getStatus())) {
                            e.setStatus("AVAILABLE");
                            earningsRepository.save(e);
                        }
                    });
                }
            }
        }

        settlement.setStatus("FAILED");
        DriverSettlement saved = settlementsRepository.save(settlement);

        List<DriverSettlementAttempt> attempts = attemptsRepository.findBySettlementIdOrderByTriedAtDesc(settlementId);
        if (!attempts.isEmpty()) {
            DriverSettlementAttempt latest = attempts.get(0);
            if ("PENDING".equals(latest.getStatus())) {
                latest.setStatus("FAILED");
                latest.setErrorMessage(errorMessage);
                latest.setCompletedAt(now);
                attemptsRepository.save(latest);
            }
        }

        return saved;
    }

    @Transactional
    public DriverSettlement retrySettlement(Long settlementId, Long requestingUserId, boolean isAdmin, boolean succeed, String gatewayRef) {
        DriverSettlement settlement = settlementsRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));
        if ("COMPLETED".equals(settlement.getStatus())) {
            throw new BadRequestException("Settlement already completed; duplicate transfer prevented");
        }
        DriverProfile driver = driverProfileRepository.findById(settlement.getDriverProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        if (!isAdmin && !driver.getUser().getId().equals(requestingUserId)) {
            throw new UnauthorizedException("You can only retry your own settlements");
        }

        // Re-reserve AVAILABLE earnings if the previous attempt failed and released them
        if ("FAILED".equals(settlement.getStatus())) {
            List<DriverEarnings> availableRows = earningsRepository.findByDriverProfileIdOrderByAccruedAtDesc(driver.getId())
                    .stream()
                    .filter(e -> "AVAILABLE".equals(e.getStatus()) && e.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            BigDecimal totalAvailable = availableRows.stream().map(DriverEarnings::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalAvailable.compareTo(settlement.getTotalAmount()) < 0) {
                throw new BadRequestException("Insufficient available balance to retry settlement");
            }
            settlement.setStatus("PENDING");
            settlementsRepository.save(settlement);
            reserveAvailableEarnings(driver.getId(), settlement.getId(), settlement.getTotalAmount(), availableRows);
        }

        int nextAttempt = attemptsRepository.findBySettlementIdOrderByTriedAtDesc(settlementId).stream()
                .mapToInt(DriverSettlementAttempt::getAttemptNumber)
                .max()
                .orElse(0) + 1;

        DriverSettlementAttempt attempt = new DriverSettlementAttempt();
        attempt.setSettlementId(settlementId);
        attempt.setAttemptNumber(nextAttempt);
        attempt.setStatus("PENDING");
        attempt.setTriedAt(LocalDateTime.now());
        attemptsRepository.save(attempt);

        return succeed ? completeSettlement(settlementId, gatewayRef) : failSettlement(settlementId, "Retry transfer failed");
    }

    @Transactional(readOnly = true)
    public List<DriverSettlement> getDriverSettlements(Long driverUserId) {
        DriverProfile driver = driverProfileRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return settlementsRepository.findByDriverProfileIdOrderByInitiatedAtDesc(driver.getId());
    }

    @Transactional(readOnly = true)
    public List<DriverSettlementAttempt> getSettlementAttempts(Long settlementId) {
        return attemptsRepository.findBySettlementIdOrderByTriedAtDesc(settlementId);
    }
}
