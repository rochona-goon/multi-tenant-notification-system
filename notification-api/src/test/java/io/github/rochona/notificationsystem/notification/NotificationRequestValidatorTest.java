package io.github.rochona.notificationsystem.notification;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRequestValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void shouldAcceptValidDirectNotification() {
        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setSubject("Booking Confirmation");
        request.setMessage("Your booking is confirmed.");
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("user@example.com");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidTemplateNotification() {
        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setTemplateId(10L);
        request.setVariables(Map.of("userName", "Rochona"));
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("user@example.com");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectTemplateRequestWithDirectContent() {
        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setTemplateId(10L);
        request.setSubject("Booking Confirmation");
        request.setMessage("Your booking is confirmed.");
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("user@example.com");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectDirectRequestWithMissingMessage() {
        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setSubject("Booking Confirmation");
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("user@example.com");

        assertFalse(validator.validate(request).isEmpty());
    }
}