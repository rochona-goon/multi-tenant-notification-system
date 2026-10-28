package io.github.rochona.notificationsystem.notification.controller;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.service.NotificationService;
import io.github.rochona.notificationsystem.shared.ResponseStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService){
        this.notificationService = notificationService;
    }

    @PostMapping("/notify")
    public ResponseEntity<NotificationResponseDTO> sendNotification(@Valid @RequestBody NotificationRequestDTO requestDTO){
        NotificationResponseDTO responseDTO = new NotificationResponseDTO();

            Notification notification = notificationService.createNotification(requestDTO.getSubject(), requestDTO.getMessage(), requestDTO.getRecipient());

            responseDTO.setId(notification.getId());
            responseDTO.setSubject(notification.getSubject());
            responseDTO.setMessage(notification.getMessage());
            responseDTO.setRecipient(notification.getRecipient());
            responseDTO.setNotificationStatus(notification.getStatus());
            responseDTO.setResponseStatus(ResponseStatus.SUCCESS);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);


    }
}
