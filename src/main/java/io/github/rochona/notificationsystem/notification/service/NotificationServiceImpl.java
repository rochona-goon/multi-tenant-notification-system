package io.github.rochona.notificationsystem.notification.service;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.sender.NotificationSender;
import io.github.rochona.notificationsystem.notification.specification.NotificationSpecification;
import io.github.rochona.notificationsystem.preference.service.PreferenceService;
import io.github.rochona.notificationsystem.template.model.Template;
import io.github.rochona.notificationsystem.template.service.TemplateRenderer;
import io.github.rochona.notificationsystem.template.service.TemplateService;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final PreferenceService preferenceService;


    @Override
    public Notification createNotification(NotificationRequestDTO request,
                                           Tenant tenant) {
        // Validations

        String subject;
        String message;
        String htmlMessage = null;

        if (request.getTemplateId() != null) {

            Template template = templateService.getTemplateEntityById(
                    request.getTemplateId(), tenant
            );

            subject = templateRenderer.render(
                    template.getSubject(), request.getVariables()
            );

            message = templateRenderer.render(
                    template.getBodyText(), request.getVariables()
            );

            htmlMessage = templateRenderer.render(
                    template.getBodyHtml(), request.getVariables()
            );

        } else {
            subject = request.getSubject();
            message = request.getMessage();
        }


        Notification notification = new Notification();
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setHtmlMessage(htmlMessage);
        notification.setRecipient(request.getRecipient());
        notification.setChannel(request.getChannel());
        notification.setUserId(request.getUserId());
        notification.setTenant(tenant);

        if (request.getUserId() != null && !request.getUserId().isBlank() &&
        !preferenceService.isChannelEnabled(
                        tenant.getId(),
                        request.getUserId(),
                        request.getChannel()
                )) {

            notification.setStatus(NotificationStatus.SUPPRESSED);

            return notificationRepository.save(notification);
        }

        Notification savedNotification = notificationRepository.save(notification);

        try{
            notificationSender.send(savedNotification);
            savedNotification.setStatus(NotificationStatus.SENT);

        } catch (Exception e) {
            log.error("Failed to send notification with id: {}", savedNotification.getId(), e);
            savedNotification.setStatus(NotificationStatus.FAILED);
        }


        return notificationRepository.save(savedNotification);
    }



    @Override
    public Page<NotificationResponseDTO> getAllNotifications(Tenant tenant, NotificationStatus status,
                                                                 NotificationChannel channel, String userId,
                                                                 LocalDate fromDate, LocalDate toDate,
                                                                 Pageable pageable) {

            Specification<Notification> specification = NotificationSpecification.belongsToTenant(tenant.getId());

            if(status != null){
                specification = specification.and(NotificationSpecification.hasStatus(status));
            }

            if(channel != null){
                specification = specification.and(NotificationSpecification.hasChannel(channel));
            }

            if(userId != null){
                specification = specification.and(NotificationSpecification.hasUserId(userId));
            }

            if (fromDate != null) {
                specification = specification.and(
                        NotificationSpecification.createdFrom(fromDate)
                );
            }

            if (toDate != null) {
                specification = specification.and(
                        NotificationSpecification.createdUntil(toDate)
                );
            }

            return notificationRepository.findAll(specification, pageable).map(this::toResponseDTO);
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
        response.setChannel(notification.getChannel());
        response.setUserId(notification.getUserId());
        response.setHtmlMessage(notification.getHtmlMessage());
        response.setNotificationStatus(notification.getStatus());

        return response;

    }

    private List<NotificationResponseDTO> toResponseDTO(List<Notification> notifications){
        return notifications.stream().map(this::toResponseDTO).toList();
    }
}
