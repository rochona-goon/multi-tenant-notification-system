package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.tenant.Tenant;

public interface NotificationService {
    Notification createNotification(String subject, String message, String recipient, Tenant tenant);
}
