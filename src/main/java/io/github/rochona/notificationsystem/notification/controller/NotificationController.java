package io.github.rochona.notificationsystem.notification.controller;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.service.NotificationService;
import io.github.rochona.notificationsystem.shared.ResponseStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

            Notification notification = notificationService.createNotification(requestDTO, tenant);

            responseDTO.setId(notification.getId());
            responseDTO.setSubject(notification.getSubject());
            responseDTO.setMessage(notification.getMessage());
            responseDTO.setHtmlMessage(notification.getHtmlMessage());
            responseDTO.setRecipient(notification.getRecipient());
            responseDTO.setNotificationStatus(notification.getStatus());
            responseDTO.setChannel(notification.getChannel());
            responseDTO.setUserId(notification.getUserId());

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }




    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getAllNotifications(@RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) NotificationStatus status,
             @RequestParam(required = false) NotificationChannel channel, @RequestParam(required = false) String userId,
             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate, HttpServletRequest request){

        if(page < 0){
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if(size < 1 || size > 100){
            throw new IllegalArgumentException("Page size must be between 1 to 100");
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Tenant tenant = (Tenant) request.getAttribute("authenticatedTenant");

        return ResponseEntity.ok(notificationService
                .getAllNotifications(tenant,  status, channel, userId, fromDate, toDate, pageable));
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
