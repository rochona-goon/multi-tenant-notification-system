package io.github.rochona.notificationsystem.notification.dto;

import io.github.rochona.notificationsystem.tenant.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequestDTO {

    private String subject;

    @NotBlank(message = "Message must not be blank")
    private String message;

    @NotBlank(message = "Recipient must not be blank")
    @Email(message = "Please provide a valid email")
    private String recipient;
}
