package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.sender.NotificationSender;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public List<NotificationResponseDTO> getAllNotifications(Tenant tenant) {

        return toResponseDTO(notificationRepository.findAllByTenant(tenant));
    }

    @Override
    public NotificationResponseDTO getNotificationById(Long id, Tenant tenant) {

        return toResponseDTO(notificationRepository.findByIdAndTenant(id, tenant).orElseThrow(() ->
                new NotificationNotFoundException("Notification not found")));
    }

    @Override
    public boolean deleteNotificationById(Long id, Tenant tenant) {
        Notification notification = notificationRepository.findByIdAndTenant(id, tenant).orElseThrow(() ->
                new NotificationNotFoundException("Notification not found"));

        if(notification.getStatus() != NotificationStatus.PENDING){
            throw new NotificationNotDeletableException("Only PENDING notifications can be deleted!");
        }

        boolean deleted = notificationRepository.deleteByIdAndTenantAndStatus(id, tenant, NotificationStatus.PENDING);

        if(!deleted){
            throw new NotificationNotDeletableException("Notification could not be deleted");
        }

        return true;
    }

    private NotificationResponseDTO toResponseDTO(Notification notification){

        NotificationResponseDTO response = new NotificationResponseDTO();
        response.setId(notification.getId());
        response.setSubject(notification.getSubject());
        response.setRecipient(notification.getRecipient());
        response.setMessage(notification.getMessage());
        response.setNotificationStatus(notification.getStatus());

        return response;

    }

    private List<NotificationResponseDTO> toResponseDTO(List<Notification> notifications){
        return notifications.stream().map(this::toResponseDTO).toList();
    }
}
