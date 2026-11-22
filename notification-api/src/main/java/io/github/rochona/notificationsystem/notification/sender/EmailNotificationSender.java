package io.github.rochona.notificationsystem.notification.sender;

import io.github.rochona.notificationsystem.notification.model.Notification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender{

    private final JavaMailSender javaMailSender;

    public EmailNotificationSender(JavaMailSender javaMailSender){
        this.javaMailSender = javaMailSender;
    }


    @Override
    public void send(Notification notification) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("notifications@localhost");
        message.setSubject(notification.getSubject());
        message.setText(notification.getMessage());
        message.setTo(notification.getRecipient());

        javaMailSender.send(message);

    }
}
