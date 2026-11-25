package com.drivio.service;

import com.drivio.entity.Notification;
import com.drivio.entity.User;
import com.drivio.enums.NotificationType;
import com.drivio.exception.ResourceNotFoundException;
import com.drivio.notification.ExternalNotificationProvider;
import com.drivio.repository.NotificationRepository;
import com.drivio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ExternalNotificationProvider externalNotificationProvider;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate,
            ExternalNotificationProvider externalNotificationProvider) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.externalNotificationProvider = externalNotificationProvider;
    }

    @Transactional
    public Notification sendNotification(Long userId, String title, String message, NotificationType type, Long referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Notification notification = new Notification(user, title, message, type, referenceId);
        Notification saved = notificationRepository.save(notification);

        // Push real-time WebSocket notification
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", saved.getId());
            payload.put("title", saved.getTitle());
            payload.put("message", saved.getMessage());
            payload.put("type", saved.getNotificationType().name());
            payload.put("referenceId", saved.getReferenceId());
            payload.put("createdAt", saved.getCreatedAt().toString());

            messagingTemplate.convertAndSend("/topic/notifications/" + userId, payload);
        } catch (Exception e) {
            logger.warn("Could not dispatch WebSocket notification to user {}: {}", userId, e.getMessage());
        }

        // Dispatch SMS via configured provider for critical trip/payment events
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            try {
                externalNotificationProvider.sendSms(user.getPhoneNumber(), "[DRIVIO] " + title + ": " + message);
            } catch (Exception e) {
                logger.warn("External SMS dispatch failed for user {}: {}", userId, e.getMessage());
            }
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (notification.getUser().getId().equals(userId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
}

