package io.github.rochona.notificationsystem.notification.controller;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.service.NotificationService;
import io.github.rochona.notificationsystem.shared.ResponseStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService){
        this.notificationService = notificationService;
    }

    @PostMapping("/create")
    public ResponseEntity<NotificationResponseDTO> sendNotification(@Valid @RequestBody NotificationRequestDTO requestDTO, HttpServletRequest request){
        NotificationResponseDTO responseDTO = new NotificationResponseDTO();
        Tenant tenant = (Tenant) request.getAttribute("authenticatedTenant");

            Notification notification = notificationService.createNotification(requestDTO.getSubject(), requestDTO.getMessage(), requestDTO.getRecipient(), tenant);

            responseDTO.setId(notification.getId());
            responseDTO.setSubject(notification.getSubject());
            responseDTO.setMessage(notification.getMessage());
            responseDTO.setRecipient(notification.getRecipient());
            responseDTO.setNotificationStatus(notification.getStatus());
            responseDTO.setResponseStatus(ResponseStatus.SUCCESS);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getAllNotifications( HttpServletRequest request){
        Tenant tenant = (Tenant) request.getAttribute("authenticatedTenant");
        return ResponseEntity.ok(notificationService.getAllNotifications(tenant));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDTO> getNotificationById(@PathVariable Long id, HttpServletRequest request){
        Tenant tenant = (Tenant) request.getAttribute("authenticatedTenant");
        return ResponseEntity.ok(notificationService.getNotificationById(id, tenant));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, HttpServletRequest request){
        Tenant tenant = (Tenant) request.getAttribute("authenticatedTenant");
        notificationService.deleteNotificationById(id, tenant);
        return ResponseEntity.noContent().build();
    }
}
