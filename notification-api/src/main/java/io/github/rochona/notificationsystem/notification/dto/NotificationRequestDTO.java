package io.github.rochona.notificationsystem.notification.dto;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@ValidNotificationRequest
public class NotificationRequestDTO {

    private String subject;

//    @NotBlank(message = "Message must not be blank")
    private String message;

    @Size(max = 100, message = "User ID cannot exceed 100 characters")
    private String userId;

    @NotNull(message = "Channel must be provided")
    private NotificationChannel channel;

    @NotBlank(message = "Recipient must not be blank")
    @Email(message = "Please provide a valid email")
    private String recipient;

    private Long templateId;

    private Map<String, Object> variables;

}
