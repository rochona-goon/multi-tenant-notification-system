package io.github.rochona.notificationsystem.notification;

import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.sender.NotificationSender;
import io.github.rochona.notificationsystem.notification.service.NotificationServiceImpl;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    private Tenant tenant;

    @BeforeEach
    void setUp(){
        tenant = new Tenant();
        tenant.setId(1L);
//        tenant.setApiKeyHash("test-api-hash");
        tenant.setName("Test Tenant");
    }


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


    @Test
    void shouldReturnAllNotificationsForTenants(){

        Notification response1 = new Notification();
        response1.setId(1L);
        response1.setSubject("Test Subject 1");
        response1.setMessage("Test Message 1");
        response1.setRecipient("testrecipient@gmail.com");
        response1.setStatus(NotificationStatus.PENDING);
        response1.setTenant(tenant);

        Notification response2 = new Notification();
        response2.setId(2L);
        response2.setSubject("Test Subject 2");
        response2.setMessage("Test Message 2");
        response2.setRecipient("testrecipient@gmail.com");
        response2.setStatus(NotificationStatus.PENDING);
        response2.setTenant(tenant);


        when(notificationRepository.findAllByTenant(any(Tenant.class)))
                .thenReturn(List.of(response1, response2));

        // Act

        List<NotificationResponseDTO> result = notificationService.getAllNotifications(tenant);

        // Assert

        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).getId());
        assertEquals("Test Subject 1", result.get(0).getSubject());
        assertEquals("testrecipient@gmail.com", result.get(0).getRecipient());
        assertEquals("Test Message 1", result.get(0).getMessage());
        assertEquals(NotificationStatus.PENDING, result.get(0).getNotificationStatus());

        assertEquals(2L, result.get(1).getId());
        assertEquals("Test Subject 2", result.get(1).getSubject());
        assertEquals("testrecipient@gmail.com", result.get(1).getRecipient());
        assertEquals("Test Message 2", result.get(1).getMessage());
        assertEquals(NotificationStatus.PENDING, result.get(1).getNotificationStatus());

        verify(notificationRepository).findAllByTenant(tenant);

    }


    @Test
    void shouldReturnEmptyListWhenTenantHasNoNotifications(){

        // Arrange

        when(notificationRepository.findAllByTenant(tenant))
                .thenReturn(List.of());

        // Act

        List<NotificationResponseDTO> result = notificationService.getAllNotifications(tenant);

        // Assert

        assertNotNull(result);
        assertEquals(0, result.size());


        // verify the call
        verify(notificationRepository).findAllByTenant(tenant);
    }


    @Test
    void shouldReturnNotificationWhenIdAndTenantMatch(){

        // Arrange

        Notification response = new Notification();
        response.setId(1L);
        response.setSubject("Test Subject");
        response.setMessage("Test Message");
        response.setRecipient("goon.rochona@gmail.com");
        response.setStatus(NotificationStatus.PENDING);
        response.setTenant(tenant);

        when(notificationRepository.findByIdAndTenant(1L, tenant))
                .thenReturn(Optional.of(response));


        // Act

        NotificationResponseDTO result = notificationService.getNotificationById(1L, tenant);

        // Assert

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("Test Message", result.getMessage());
        assertEquals("goon.rochona@gmail.com", result.getRecipient());
        assertEquals(NotificationStatus.PENDING, result.getNotificationStatus());


        // Verify the call
        verify(notificationRepository).findByIdAndTenant(1L, tenant);
    }


    @Test
    void shouldThrowExceptionWhenNotificationDoesNotBelongToTenant(){

        // Arrange

        when(notificationRepository.findByIdAndTenant(1L, tenant))
                .thenReturn(Optional.empty());


        // Act & Assert

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.getNotificationById(1L, tenant)
        );


        // Verify the call

        verify(notificationRepository).findByIdAndTenant(1L, tenant);
    }


    @Test
    void shouldDeleteAPendingNotificationSuccessfully(){

        // Arrange

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setTenant(tenant);

        when(notificationRepository.findByIdAndTenant(1L, tenant))
                .thenReturn(Optional.of(notification));

        when(notificationRepository.deleteByIdAndTenantAndStatus(1L, tenant, NotificationStatus.PENDING))
                .thenReturn(true);


        // Act
        boolean deleted = notificationService.deleteNotificationById(1L, tenant);



        // Assert
        assertTrue(deleted);


        // Verifying the calls
        verify(notificationRepository).findByIdAndTenant(1L, tenant);

        verify(notificationRepository).deleteByIdAndTenantAndStatus(1L, tenant, NotificationStatus.PENDING);

    }


    @ParameterizedTest
    @EnumSource(
            value = NotificationStatus.class,
            names = {"SENT", "FAILED"}
    )
    void shouldThrowExceptionWhenDeletingNonPendingNotification(NotificationStatus status){

        // Arrange

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setStatus(status);
        notification.setTenant(tenant);

        when(notificationRepository.findByIdAndTenant(1L, tenant))
                .thenReturn(Optional.of(notification));


        // Act & Assert

        assertThrows(
                NotificationNotDeletableException.class,
                () -> notificationService.deleteNotificationById(1L, tenant)
        );


        // Verifying the calls

        verify(notificationRepository).findByIdAndTenant(1L, tenant);

        verify(notificationRepository, never())
                .deleteByIdAndTenantAndStatus(1L, tenant, NotificationStatus.PENDING);

    }



    @Test
    void shouldThrowExceptionWhenDeletingNotificationOfAnotherTenantRepoLevel(){
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setTenant(tenant);

        Tenant tenant1 = new Tenant();
        tenant1.setName("Another Tenant");
        tenant1.setId(2L);

        when(notificationRepository.findByIdAndTenant(1L, tenant1))
                .thenReturn(Optional.of(notification));

        when(notificationRepository.deleteByIdAndTenantAndStatus(1L, tenant1, NotificationStatus.PENDING))
                .thenThrow(new NotificationNotDeletableException("Notification does not belong to this tenant"));


        // Act & Assert

        assertThrows(
                NotificationNotDeletableException.class,
                () -> notificationService.deleteNotificationById(1L, tenant1)
        );


        // verify the call
        verify(notificationRepository).deleteByIdAndTenantAndStatus(1L, tenant1, NotificationStatus.PENDING);
    }


    @Test
    void shouldThrowExceptionWhenDeletingNotificationOfAnotherTenant(){

        // Arrange

        Tenant tenant1 = new Tenant();
        tenant1.setName("Another Tenant");
        tenant1.setId(2L);

        when(notificationRepository.findByIdAndTenant(1L, tenant1))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.deleteNotificationById(1L, tenant1)
        );


        // Verifying the calls
        verify(notificationRepository).findByIdAndTenant(1L, tenant1);
        verify(notificationRepository, never()).deleteByIdAndTenantAndStatus(1L, tenant1, NotificationStatus.PENDING);

    }
}











