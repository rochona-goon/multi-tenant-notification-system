package io.github.rochona.notificationsystem.notification.dto;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.shared.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationResponseDTO {
    private Long id;
    private String userId;
    private String subject;
    private String message;
    private String htmlMessage;
    private String recipient;
    private NotificationChannel channel;
    private NotificationStatus notificationStatus;
}
