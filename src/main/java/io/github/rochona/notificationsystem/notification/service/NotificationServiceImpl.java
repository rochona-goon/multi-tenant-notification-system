package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.sender.NotificationSender;
import io.github.rochona.notificationsystem.tenant.Tenant;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public NotificationServiceImpl(NotificationRepository notificationRepository, NotificationSender notificationSender){
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    @Override
    public Notification createNotification(String subject, String message, String recipient, Tenant tenant) {
        // Validations

        Notification notification = new Notification();
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setRecipient(recipient);
        notification.setTenant(tenant);

        Notification savedNotification = notificationRepository.save(notification);

        try{
            notificationSender.send(notification);
            savedNotification.setStatus(NotificationStatus.SENT);

        } catch (Exception e) {
            savedNotification.setStatus(NotificationStatus.FAILED);
        }


        return notificationRepository.save(savedNotification);
    }
}
