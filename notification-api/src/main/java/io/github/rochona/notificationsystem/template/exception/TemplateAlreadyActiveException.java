package io.github.rochona.notificationsystem.template.exception;

public class TemplateAlreadyActiveException extends RuntimeException {
    public TemplateAlreadyActiveException(String message) {
        super(message);
    }
}
