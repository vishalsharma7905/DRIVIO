package com.drivio.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentOrderResult createOrder(String bookingReference, BigDecimal amountInr);
    boolean verifySignature(String orderId, String paymentId, String signature);
    String getProviderName();

    record PaymentOrderResult(String orderId, String currency, BigDecimal amount, String keyId, boolean sandboxMode) {}
}
