package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.tenant.model.Tenant;

import java.util.List;

public interface NotificationService {
    Notification createNotification(String subject, String message, String recipient, Tenant tenant);

    List<NotificationResponseDTO> getAllNotifications(Tenant tenant);

    NotificationResponseDTO getNotificationById(Long id, Tenant tenant);

    boolean deleteNotificationById(Long id, Tenant tenant);
}
