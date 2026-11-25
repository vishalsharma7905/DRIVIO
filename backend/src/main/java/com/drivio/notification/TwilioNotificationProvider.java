package com.drivio.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@ConditionalOnProperty(name = "drivio.notifications.provider", havingValue = "twilio")
public class TwilioNotificationProvider implements ExternalNotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(TwilioNotificationProvider.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final RestTemplate restTemplate = new RestTemplate();

    public TwilioNotificationProvider(
            @Value("${drivio.notifications.twilio.account-sid:}") String accountSid,
            @Value("${drivio.notifications.twilio.auth-token:}") String authToken,
            @Value("${drivio.notifications.twilio.from-number:}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        dispatch(phoneNumber, message, false);
    }

    @Override
    public void sendWhatsApp(String phoneNumber, String message) {
        dispatch(phoneNumber, message, true);
    }

    private void dispatch(String to, String body, boolean whatsapp) {
        if (accountSid.isBlank() || authToken.isBlank() || to == null || to.isBlank()) {
            logger.warn("Twilio credentials or recipient missing; skipping dispatch to {}", to);
            return;
        }
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + auth);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", whatsapp ? "whatsapp:" + to : to);
            form.add("From", whatsapp ? "whatsapp:" + fromNumber : fromNumber);
            form.add("Body", body);

            restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
        } catch (Exception ex) {
            logger.warn("Twilio dispatch failed for {}: {}", to, ex.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "TWILIO";
    }
}
