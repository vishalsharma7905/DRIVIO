package com.drivio.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class RazorpayPaymentGateway implements PaymentGateway {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayPaymentGateway.class);

    private final String keyId;
    private final String keySecret;
    private final RestTemplate restTemplate = new RestTemplate();

    public RazorpayPaymentGateway(
            @Value("${drivio.payment.razorpay.key-id:}") String keyId,
            @Value("${drivio.payment.razorpay.key-secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public PaymentOrderResult createOrder(String bookingReference, BigDecimal amountInr) {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            String sandboxOrderId = "order_sandbox_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            logger.info("Razorpay keys not configured; generated sandbox order {} for {}", sandboxOrderId, bookingReference);
            return new PaymentOrderResult(sandboxOrderId, "INR", amountInr, "rzp_test_sandbox", true);
        }

        try {
            long amountPaise = amountInr.multiply(new BigDecimal("100")).longValue();
            Map<String, Object> body = new HashMap<>();
            body.put("amount", amountPaise);
            body.put("currency", "INR");
            body.put("receipt", bookingReference);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + auth);

            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    "https://api.razorpay.com/v1/orders",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            String orderId = resp.getBody() != null ? String.valueOf(resp.getBody().get("id")) : "order_" + UUID.randomUUID();
            return new PaymentOrderResult(orderId, "INR", amountInr, keyId, false);
        } catch (Exception ex) {
            logger.warn("Razorpay API call failed ({}), falling back to sandbox order", ex.getMessage());
            String sandboxOrderId = "order_sandbox_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            return new PaymentOrderResult(sandboxOrderId, "INR", amountInr, keyId, true);
        }
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (keySecret == null || keySecret.isBlank()) {
            return orderId != null && orderId.startsWith("order_sandbox_") && paymentId != null && !paymentId.isBlank();
        }
        try {
            String payload = orderId + "|" + paymentId;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString().equals(signature);
        } catch (Exception e) {
            logger.error("Razorpay signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }
}
