package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.model.Notification;

public interface NotificationService {
    Notification createNotification(String subject, String message, String recipient);
}
