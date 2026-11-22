package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface NotificationService {
    Notification createNotification(
            NotificationRequestDTO request,
            Tenant tenant
    );

    Page<NotificationResponseDTO> getAllNotifications(Tenant tenant,
                                                      NotificationStatus status,
                                                      NotificationChannel channel,
                                                      String userId,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,Pageable pageable);

    NotificationResponseDTO getNotificationById(Long id, Tenant tenant);

    boolean deleteNotificationById(Long id, Tenant tenant);
}
