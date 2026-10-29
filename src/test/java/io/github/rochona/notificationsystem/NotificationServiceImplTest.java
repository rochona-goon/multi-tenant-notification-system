package io.github.rochona.notificationsystem;

import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.sender.NotificationSender;
import io.github.rochona.notificationsystem.notification.service.NotificationServiceImpl;
import io.github.rochona.notificationsystem.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;


    @Test
    void shouldSaveAndReturnNotificationWhenEmailIsSent(){
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setSubject("Hi");
        savedNotification.setMessage("Hi there, how are you doing?");
        savedNotification.setRecipient("goon.rochona@gmail.com");
        savedNotification.setStatus(NotificationStatus.PENDING);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);

                    if(notification.getId() == null)
                        notification.setId(1L);

                    return notification;
                });

        Tenant tenant = new Tenant();
        tenant.setName("Test tenant");
        tenant.setId(1L);

        // Act

        Notification result = notificationService.createNotification(
                "Hi",
                "Hi there, how are you doing?",
                "goon.rochona@gmail.com",
                tenant
        );

        // Assert

        assertNotNull(result);
        assertEquals(1L,result.getId());
        assertEquals("Hi", result.getSubject());
        assertEquals("Hi there, how are you doing?", result.getMessage());
        assertEquals("goon.rochona@gmail.com", result.getRecipient());
        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertSame(tenant, result.getTenant());

        InOrder inOrder = inOrder(notificationRepository, notificationSender);

        inOrder.verify(notificationRepository).save(any(Notification.class));
        inOrder.verify(notificationSender).send(any(Notification.class));
        inOrder.verify(notificationRepository).save(any(Notification.class));

    }


    @Test
    void shouldMarkNotificationAsFailedWhenEmailIsSendingFails(){
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setSubject("Hi");
        savedNotification.setMessage("Hi there, how are you doing?");
        savedNotification.setRecipient("goon.rochona@gmail.com");
        savedNotification.setStatus(NotificationStatus.PENDING);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);

                    if(notification.getId() == null)
                        notification.setId(1L);

                    return notification;
                });

        doThrow(new RuntimeException("Email Delivery Failed"))
                .when(notificationSender)
                .send(any(Notification.class));

        Tenant tenant = new Tenant();
        tenant.setName("Test tenant");
        tenant.setId(1L);

        // Act

        Notification result = notificationService.createNotification(
                "Hello",
                "Hello there, how are you doing?",
                "goon.rochona@gmail.com",
                tenant
        );

        // Assert

        assertNotNull(result);
        assertEquals(NotificationStatus.FAILED, result.getStatus());

        InOrder inOrder = inOrder(notificationRepository, notificationSender);

        inOrder.verify(notificationRepository).save(any(Notification.class));
        inOrder.verify(notificationSender).send(any(Notification.class));
        inOrder.verify(notificationRepository).save(any(Notification.class));

    }
}
