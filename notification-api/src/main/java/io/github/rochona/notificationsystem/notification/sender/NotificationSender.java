package io.github.rochona.notificationsystem.notification.sender;

import io.github.rochona.notificationsystem.notification.model.Notification;

public interface NotificationSender {

    void send(Notification notification);
}
