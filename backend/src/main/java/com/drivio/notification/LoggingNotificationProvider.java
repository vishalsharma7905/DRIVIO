package com.drivio.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "drivio.notifications.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingNotificationProvider implements ExternalNotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationProvider.class);

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            logger.info("[SMS-DEV] To {}: {}", phoneNumber, message);
        }
    }

    @Override
    public void sendWhatsApp(String phoneNumber, String message) {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            logger.info("[WHATSAPP-DEV] To {}: {}", phoneNumber, message);
        }
    }

    @Override
    public String getProviderName() {
        return "LOGGING";
    }
}
