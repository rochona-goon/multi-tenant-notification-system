package io.github.rochona.notificationsystem.notification.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotificationRequestValidator.class)
@Documented
public @interface ValidNotificationRequest {

    String message() default "Invalid notification request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
