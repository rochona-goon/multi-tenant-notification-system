package io.github.rochona.notificationsystem.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
public class MailpitIntegrationTest {

    @Autowired
    private JavaMailSender javaMailSender;

    @Test
    void shouldSendMailToMailpit(){

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("notifications@localhost");
        message.setTo("goon.rochona@gmail.com");
        message.setSubject("Mailpit Integration Test");
        message.setText("Hello! This email was sent from our Spring Boot application.");

        javaMailSender.send(message);

    }
}
