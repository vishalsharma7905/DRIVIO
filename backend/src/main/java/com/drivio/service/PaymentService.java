package com.drivio.service;

import com.drivio.dto.PaymentResponseDto;
import com.drivio.dto.ProcessPaymentRequest;
import com.drivio.entity.Booking;
import com.drivio.entity.DriverEarnings;
import com.drivio.entity.FinancialTransaction;
import com.drivio.entity.Payment;
import com.drivio.entity.User;
import com.drivio.enums.NotificationType;
import com.drivio.enums.PaymentMethod;
import com.drivio.enums.PaymentStatus;
import com.drivio.enums.Role;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.exception.UnauthorizedException;
import com.drivio.payment.PaymentGateway;
import com.drivio.repository.BookingRepository;
import com.drivio.repository.DriverEarningsRepository;
import com.drivio.repository.FinancialTransactionRepository;
import com.drivio.repository.PaymentRepository;
import com.drivio.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentGateway paymentGateway;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final DriverEarningsRepository driverEarningsRepository;
    private final DriverEarningsService driverEarningsService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            PaymentGateway paymentGateway,
            FinancialTransactionRepository financialTransactionRepository,
            DriverEarningsRepository driverEarningsRepository,
            DriverEarningsService driverEarningsService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.paymentGateway = paymentGateway;
        this.financialTransactionRepository = financialTransactionRepository;
        this.driverEarningsRepository = driverEarningsRepository;
        this.driverEarningsService = driverEarningsService;
    }

    @Transactional
    public PaymentResponseDto processPayment(Long customerUserId, ProcessPaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerUserId));

        if (!booking.getCustomer().getId().equals(customerUserId)) {
            throw new BadRequestException("You are not authorized to make payment for this booking");
        }

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> new Payment(booking, customer, booking.getTotalFare(), request.getPaymentMethod()));

        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Payment for this booking has already been settled");
        }

        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setAmount(booking.getTotalFare());
        payment.setTaxAmount(booking.getTaxAmount());
        payment.setPaidAt(LocalDateTime.now());

        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String txId = "TXN-" + datePrefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String receipt = "RCP-" + datePrefix + "-" + booking.getId();
        payment.setTransactionId(txId);
        payment.setReceiptNumber(receipt);

        Payment saved = paymentRepository.save(payment);

        // Append financial transaction to the append-only ledger (idempotent per booking capture)
        String idempotencyKey = "payment_capture_" + booking.getId() + "_" + saved.getId();
        if (financialTransactionRepository.findByIdempotencyKey(idempotencyKey).isEmpty()) {
            FinancialTransaction financialTransaction = new FinancialTransaction();
            financialTransaction.setBookingId(booking.getId());
            financialTransaction.setDriverProfileId(booking.getDriver() != null ? booking.getDriver().getId() : null);
            financialTransaction.setTransactionId(saved.getTransactionId());
            financialTransaction.setGatewayEventId("evt_" + booking.getId() + "_payment_" + saved.getId());
            financialTransaction.setEventType("PAYMENT_SUCCESS");
            financialTransaction.setAmount(saved.getAmount());
            financialTransaction.setCurrency("INR");
            financialTransaction.setStatus("COMPLETED");
            financialTransaction.setCompletedAt(LocalDateTime.now());
            financialTransaction.setIdempotencyKey(idempotencyKey);
            financialTransactionRepository.save(financialTransaction);
        }

        driverEarningsService.releaseAccruedToAvailableForBooking(booking.getId());

        notificationService.sendNotification(
                customerUserId,
                "Payment Successful (₹" + saved.getAmount() + ")",
                "Your payment of ₹" + saved.getAmount() + " via " + saved.getPaymentMethod() + " was processed successfully. Receipt: " + receipt,
                NotificationType.PAYMENT_CONFIRMED,
                booking.getId()
        );

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByBooking(Long bookingId, Long requestingUserId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for booking: " + bookingId));
        Booking booking = payment.getBooking();
        boolean isCustomer = booking.getCustomer().getId().equals(requestingUserId);
        boolean isDriver = booking.getDriver() != null && booking.getDriver().getUser().getId().equals(requestingUserId);
        boolean isAdmin = userRepository.findById(requestingUserId)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);

        if (!isCustomer && !isDriver && !isAdmin) {
            throw new UnauthorizedException("Access denied: You are not authorized to view this payment receipt");
        }
        return mapToDto(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getCustomerPayments(Long customerUserId) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerUserId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private PaymentResponseDto mapToDto(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId());
        dto.setBookingId(payment.getBooking().getId());
        dto.setBookingReference(payment.getBooking().getBookingReference());
        dto.setCustomerId(payment.getCustomer().getId());
        dto.setCustomerName(payment.getCustomer().getFullName());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentStatus(payment.getPaymentStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setAmount(payment.getAmount());
        dto.setTaxAmount(payment.getTaxAmount());
        dto.setPaidAt(payment.getPaidAt());
        dto.setReceiptNumber(payment.getReceiptNumber());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    @Transactional
    public PaymentGateway.PaymentOrderResult createRazorpayOrder(Long customerUserId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getCustomer().getId().equals(customerUserId)) {
            throw new UnauthorizedException("You are not authorized to initiate payment for this booking");
        }
        PaymentGateway.PaymentOrderResult order = paymentGateway.createOrder(booking.getBookingReference(), booking.getTotalFare());
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> new Payment(booking, booking.getCustomer(), booking.getTotalFare(), PaymentMethod.RAZORPAY));
        payment.setGatewayOrderId(order.orderId());
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        paymentRepository.save(payment);
        return order;
    }

    @Transactional
    public PaymentResponseDto verifyRazorpayPayment(Long customerUserId, Long bookingId, String orderId, String paymentId, String signature) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getCustomer().getId().equals(customerUserId)) {
            throw new UnauthorizedException("You are not authorized to verify payment for this booking");
        }
        if (!paymentGateway.verifySignature(orderId, paymentId, signature)) {
            throw new BadRequestException("Invalid Razorpay payment signature");
        }
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Payment already settled");
        }
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setGatewayOrderId(orderId);
        payment.setGatewayPaymentId(paymentId);
        payment.setGatewaySignature(signature);
        payment.setTransactionId(paymentId);
        payment.setReceiptNumber("RCP-" + datePrefix + "-" + booking.getId());
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        // Append financial transaction to the append-only ledger (idempotent per Razorpay payment)
        String idempotencyKey = "razorpay_capture_" + booking.getId() + "_" + paymentId;
        if (financialTransactionRepository.findByIdempotencyKey(idempotencyKey).isEmpty()) {
            FinancialTransaction financialTransaction = new FinancialTransaction();
            financialTransaction.setBookingId(booking.getId());
            financialTransaction.setDriverProfileId(booking.getDriver() != null ? booking.getDriver().getId() : null);
            financialTransaction.setTransactionId(saved.getTransactionId());
            financialTransaction.setGatewayEventId("evt_rzp_" + paymentId);
            financialTransaction.setGatewayPaymentId(paymentId);
            financialTransaction.setGatewaySignature(signature);
            financialTransaction.setEventType("PAYMENT_CAPTURED");
            financialTransaction.setAmount(saved.getAmount());
            financialTransaction.setCurrency("INR");
            financialTransaction.setStatus("COMPLETED");
            financialTransaction.setCompletedAt(LocalDateTime.now());
            financialTransaction.setIdempotencyKey(idempotencyKey);
            financialTransactionRepository.save(financialTransaction);
        }

        driverEarningsService.releaseAccruedToAvailableForBooking(booking.getId());

        notificationService.sendNotification(
                customerUserId,
                "Payment Successful (₹" + saved.getAmount() + ")",
                "Razorpay payment verified for booking " + booking.getBookingReference() + ". Receipt: " + saved.getReceiptNumber(),
                NotificationType.PAYMENT_CONFIRMED,
                booking.getId()
        );
        return mapToDto(saved);
    }

    /**
     * Idempotent Razorpay webhook handler. Duplicate gatewayEventId deliveries are safely ignored.
     */
    @Transactional
    public WebhookResult handleRazorpayWebhook(String eventId, Long bookingId, String orderId, String paymentId, String signature) {
        if (eventId == null || eventId.isBlank()) {
            throw new BadRequestException("gatewayEventId is required");
        }
        Optional<FinancialTransaction> existingByEvent = financialTransactionRepository.findByGatewayEventId(eventId);
        if (existingByEvent.isPresent()) {
            return new WebhookResult(true, true, existingByEvent.get().getTransactionId(), "Duplicate webhook ignored");
        }
        String idemKey = "webhook_" + eventId;
        Optional<FinancialTransaction> existingByIdem = financialTransactionRepository.findByIdempotencyKey(idemKey);
        if (existingByIdem.isPresent()) {
            return new WebhookResult(true, true, existingByIdem.get().getTransactionId(), "Duplicate webhook ignored");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!paymentGateway.verifySignature(orderId, paymentId, signature)) {
            throw new BadRequestException("Invalid Razorpay webhook signature");
        }

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> new Payment(booking, booking.getCustomer(), booking.getTotalFare(), PaymentMethod.RAZORPAY));

        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            payment.setPaymentMethod(PaymentMethod.RAZORPAY);
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setGatewayOrderId(orderId);
            payment.setGatewayPaymentId(paymentId);
            payment.setGatewaySignature(signature);
            payment.setTransactionId(paymentId);
            payment.setReceiptNumber("RCP-" + datePrefix + "-" + booking.getId());
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        FinancialTransaction tx = new FinancialTransaction();
        tx.setBookingId(booking.getId());
        tx.setDriverProfileId(booking.getDriver() != null ? booking.getDriver().getId() : null);
        tx.setTransactionId("TXN-WH-" + eventId);
        tx.setGatewayEventId(eventId);
        tx.setGatewayPaymentId(paymentId);
        tx.setGatewaySignature(signature);
        tx.setEventType("PAYMENT_CAPTURED");
        tx.setAmount(booking.getTotalFare());
        tx.setCurrency("INR");
        tx.setStatus("COMPLETED");
        tx.setCompletedAt(LocalDateTime.now());
        tx.setIdempotencyKey(idemKey);
        financialTransactionRepository.save(tx);

        driverEarningsService.releaseAccruedToAvailableForBooking(booking.getId());

        return new WebhookResult(true, false, tx.getTransactionId(), "Payment captured via webhook");
    }

    /**
     * Full or partial refund with proportional tax/commission/driver-payout allocation and idempotency.
     */
    @Transactional
    public RefundResult processRefund(Long bookingId, BigDecimal refundAmount, String idempotencyKey, boolean simulateFailure) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : "refund_" + bookingId + "_" + UUID.randomUUID();

        Optional<FinancialTransaction> existing = financialTransactionRepository.findByIdempotencyKey(effectiveKey);
        if (existing.isPresent() && "COMPLETED".equals(existing.get().getStatus())) {
            FinancialTransaction prev = existing.get();
            return new RefundResult(prev.getTransactionId(), prev.getEventType(), prev.getAmount().abs(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "COMPLETED", true);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));

        if (payment.getPaymentStatus() != PaymentStatus.PAID && payment.getPaymentStatus() != PaymentStatus.REFUNDED) {
            throw new BadRequestException("Only captured payments can be refunded");
        }

        BigDecimal captured = payment.getAmount();
        BigDecimal alreadyRefunded = financialTransactionRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(t -> ("FULL_REFUND".equals(t.getEventType()) || "PARTIAL_REFUND".equals(t.getEventType())) && "COMPLETED".equals(t.getStatus()))
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal reqRefund = refundAmount.setScale(2, RoundingMode.HALF_UP);
        if (alreadyRefunded.add(reqRefund).compareTo(captured) > 0) {
            throw new BadRequestException("Total refunds (₹" + alreadyRefunded.add(reqRefund) + ") cannot exceed captured amount (₹" + captured + ")");
        }

        if (simulateFailure) {
            String failKey = effectiveKey + "_fail_" + UUID.randomUUID().toString().substring(0, 6);
            FinancialTransaction failedTx = new FinancialTransaction();
            failedTx.setBookingId(bookingId);
            failedTx.setDriverProfileId(booking.getDriver() != null ? booking.getDriver().getId() : null);
            failedTx.setTransactionId("RFND-FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            failedTx.setEventType("REFUND_FAILED");
            failedTx.setAmount(reqRefund.negate());
            failedTx.setCurrency("INR");
            failedTx.setStatus("FAILED");
            failedTx.setIdempotencyKey(failKey);
            financialTransactionRepository.save(failedTx);
            return new RefundResult(failedTx.getTransactionId(), "REFUND_FAILED", reqRefund,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "FAILED", false);
        }

        // Proportional allocation (Spec §7): residual absorbed into platform commission
        BigDecimal ratio = reqRefund.divide(captured, 8, RoundingMode.HALF_UP);
        BigDecimal taxTotal = payment.getTaxAmount() != null ? payment.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal driverTotal = captured.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxReversal = taxTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal driverReversal = driverTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionReversal = reqRefund.subtract(taxReversal).subtract(driverReversal).setScale(2, RoundingMode.HALF_UP);

        boolean isFull = alreadyRefunded.add(reqRefund).compareTo(captured) == 0;
        String eventType = isFull ? "FULL_REFUND" : "PARTIAL_REFUND";
        String txnId = "RFND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        FinancialTransaction refundTx = new FinancialTransaction();
        refundTx.setBookingId(bookingId);
        refundTx.setDriverProfileId(booking.getDriver() != null ? booking.getDriver().getId() : null);
        refundTx.setTransactionId(txnId);
        refundTx.setEventType(eventType);
        refundTx.setAmount(reqRefund.negate());
        refundTx.setCurrency("INR");
        refundTx.setStatus("COMPLETED");
        refundTx.setCompletedAt(LocalDateTime.now());
        refundTx.setIdempotencyKey(effectiveKey);
        FinancialTransaction savedRefund = financialTransactionRepository.save(refundTx);

        if (isFull) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        if (booking.getDriver() != null && driverReversal.compareTo(BigDecimal.ZERO) > 0) {
            DriverEarnings adj = new DriverEarnings();
            adj.setDriverProfileId(booking.getDriver().getId());
            adj.setBookingId(null);
            adj.setAmount(driverReversal.negate());
            adj.setStatus("ADJUSTED");
            adj.setDescription("Refund adjustment for booking " + booking.getBookingReference() + " (ref " + savedRefund.getTransactionId() + ")");
            driverEarningsRepository.save(adj);
        }

        return new RefundResult(txnId, eventType, reqRefund, taxReversal, commissionReversal, driverReversal, "COMPLETED", false);
    }

    public record WebhookResult(boolean processed, boolean duplicateIgnored, String transactionId, String message) {}

    public record RefundResult(
            String transactionId,
            String eventType,
            BigDecimal refundAmount,
            BigDecimal taxReversal,
            BigDecimal commissionReversal,
            BigDecimal driverPayoutReversal,
            String status,
            boolean duplicateIgnored
    ) {}
}

