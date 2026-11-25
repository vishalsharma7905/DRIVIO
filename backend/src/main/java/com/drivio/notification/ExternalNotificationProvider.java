package com.drivio.notification;

public interface ExternalNotificationProvider {
    void sendSms(String phoneNumber, String message);
    void sendWhatsApp(String phoneNumber, String message);
    String getProviderName();
}
