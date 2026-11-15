package io.github.rochona.notificationsystem.notification.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotificationRequestValidator
        implements ConstraintValidator<ValidNotificationRequest, NotificationRequestDTO> {

    @Override
    public boolean isValid(
            NotificationRequestDTO request,
            ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        boolean hasTemplateId = request.getTemplateId() != null;
        boolean hasSubject = request.getSubject() != null
                && !request.getSubject().isBlank();
        boolean hasMessage = request.getMessage() != null
                && !request.getMessage().isBlank();

        boolean hasVariables = request.getVariables() != null
                && !request.getVariables().isEmpty();

        if (hasTemplateId) {
            return !hasSubject && !hasMessage;
        }

        return hasSubject && hasMessage && !hasVariables;
    }
}