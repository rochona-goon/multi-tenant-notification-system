package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.model.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository){
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification createNotification(String subject, String message, String recipient) {
        // Validations

        Notification notification = new Notification();
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setRecipient(recipient);



        return notificationRepository.save(notification);
    }
}
