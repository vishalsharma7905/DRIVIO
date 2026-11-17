package com.drivio.service;

import com.drivio.dto.PaymentResponseDto;
import com.drivio.dto.ProcessPaymentRequest;
import com.drivio.entity.Booking;
import com.drivio.entity.Payment;
import com.drivio.entity.User;
import com.drivio.enums.NotificationType;
import com.drivio.enums.PaymentStatus;
import com.drivio.exception.BadRequestException;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.repository.BookingRepository;
import com.drivio.repository.PaymentRepository;
import com.drivio.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
    public PaymentResponseDto getPaymentByBooking(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for booking: " + bookingId));
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
}

