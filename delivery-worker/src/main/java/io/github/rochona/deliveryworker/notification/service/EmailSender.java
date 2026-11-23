package io.github.rochona.deliveryworker.notification.service;

public interface EmailSender {
    void sendEmail(
            String recipient,
            String subject,
            String plainText,
            String htmlContent
    );
}
