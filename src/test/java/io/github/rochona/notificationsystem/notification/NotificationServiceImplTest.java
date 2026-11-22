package io.github.rochona.notificationsystem.notification;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.service.NotificationServiceImpl;
import io.github.rochona.notificationsystem.outbox.model.OutboxEvent;
import io.github.rochona.notificationsystem.outbox.OutboxEventRepository;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventType;
import io.github.rochona.notificationsystem.preference.service.PreferenceService;
import io.github.rochona.notificationsystem.template.exception.TemplateNotFoundException;
import io.github.rochona.notificationsystem.template.model.Template;
import io.github.rochona.notificationsystem.template.service.TemplateRenderer;
import io.github.rochona.notificationsystem.template.service.TemplateService;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

//    @Mock
//    private NotificationSender notificationSender;

    @Mock
    private TemplateService templateService;

    @Mock
    private TemplateRenderer templateRenderer;

    @Mock
    private PreferenceService preferenceService;

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


    // ----------- createNotification ------------->


    @Test
    void shouldSaveAndReturnNotificationWhenEmailIsSent(){
    // Arrange

        when(preferenceService.isChannelEnabled(
                tenant.getId(),
                "testuser101",
                NotificationChannel.EMAIL
        )).thenReturn(true);

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

        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setSubject("Hi");
        request.setMessage("Hi there, how are you doing?");
        request.setRecipient("goon.rochona@gmail.com");
        request.setUserId("testuser101");
        request.setChannel(NotificationChannel.EMAIL);

        // Act

        Notification result = notificationService.createNotification(request, tenant);

        // Assert

        assertNotNull(result);
        assertEquals(1L,result.getId());
        assertEquals("Hi", result.getSubject());
        assertEquals("Hi there, how are you doing?", result.getMessage());
        assertEquals("goon.rochona@gmail.com", result.getRecipient());
        assertEquals("testuser101", result.getUserId());
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        assertEquals(NotificationStatus.PENDING, result.getStatus());
        assertSame(tenant, result.getTenant());
        assertNull(result.getHtmlMessage());

        verify(notificationRepository).save(any(Notification.class));

        verify(outboxEventRepository).save(argThat(event ->
                event.getNotification().getId().equals(result.getId())
                        && event.getEventType() == OutboxEventType.NOTIFICATION_CREATED
                        && event.getStatus() == OutboxEventStatus.PENDING
        ));

        verifyNoInteractions(templateService, templateRenderer);

    }


    @Test
    void shouldCreateNotificationUsingTemplateSuccessfully() {

        // Arrange
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test tenant");

        Template template = new Template();
        template.setId(10L);
        template.setSubject("Hello {{name}}");
        template.setBodyText("Welcome {{name}}");
        template.setBodyHtml("<h1>Welcome {{name}}</h1>");

        Map<String, Object> variables = Map.of("name", "Rochona");

        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setTemplateId(10L);
        request.setVariables(variables);
        request.setRecipient("goon.rochona@gmail.com");
        request.setUserId("testuser101");
        request.setChannel(NotificationChannel.EMAIL);

        when(preferenceService.isChannelEnabled(
                tenant.getId(),
                "testuser101",
                NotificationChannel.EMAIL
        )).thenReturn(true);

        when(templateService.getTemplateEntityById(10L, tenant))
                .thenReturn(template);

        when(templateRenderer.renderText(template.getSubject(), variables))
                .thenReturn("Hello Rochona");

        when(templateRenderer.renderText(template.getBodyText(), variables))
                .thenReturn("Welcome Rochona");

        when(templateRenderer.renderHtml(template.getBodyHtml(), variables))
                .thenReturn("<h1>Welcome Rochona</h1>");

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);

                    if (notification.getId() == null) {
                        notification.setId(1L);
                    }

                    return notification;
                });

        // Act
        Notification result = notificationService.createNotification(request, tenant);

        // Assert
        assertNotNull(result);
        assertEquals("Hello Rochona", result.getSubject());
        assertEquals("Welcome Rochona", result.getMessage());
        assertEquals("<h1>Welcome Rochona</h1>", result.getHtmlMessage());
        assertEquals("goon.rochona@gmail.com", result.getRecipient());
        assertEquals("testuser101", result.getUserId());
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        assertEquals(NotificationStatus.PENDING, result.getStatus());
        assertSame(tenant, result.getTenant());

        verify(templateService).getTemplateEntityById(10L, tenant);

        verify(templateRenderer).renderText(template.getSubject(), variables);
        verify(templateRenderer).renderText(template.getBodyText(), variables);
        verify(templateRenderer).renderHtml(template.getBodyHtml(), variables);

        verify(outboxEventRepository).save(argThat(event ->
                event.getNotification().getId().equals(result.getId())
                        && event.getEventType() == OutboxEventType.NOTIFICATION_CREATED
                        && event.getStatus() == OutboxEventStatus.PENDING
        ));
    }


    @Test
    void shouldSaveNotificationAndOutboxEventSuccessfully() {

        // Arrange
        when(preferenceService.isChannelEnabled(
                tenant.getId(),
                "testuser101",
                NotificationChannel.EMAIL
        )).thenReturn(true);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);

                    if (notification.getId() == null) {
                        notification.setId(1L);
                    }

                    return notification;
                });

        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setSubject("Hi");
        request.setMessage("Hello there");
        request.setRecipient("test@gmail.com");
        request.setUserId("testuser101");
        request.setChannel(NotificationChannel.EMAIL);

        // Act
        Notification result =
                notificationService.createNotification(request, tenant);

        // Assert
        assertNotNull(result);
        assertEquals(NotificationStatus.PENDING, result.getStatus());

        verify(notificationRepository).save(any(Notification.class));

        verify(outboxEventRepository).save(argThat(event ->
                event.getNotification().getId().equals(result.getId())
                        && event.getEventType() == OutboxEventType.NOTIFICATION_CREATED
                        && event.getStatus() == OutboxEventStatus.PENDING
        ));
    }


    @Test
    void shouldThrowExceptionWhenTemplateIsNotFound() {

        // Arrange
        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setTemplateId(10L);
        request.setVariables(Map.of("name", "Rochona"));
        request.setRecipient("goon.rochona@gmail.com");
        request.setChannel(NotificationChannel.EMAIL);

        when(templateService.getTemplateEntityById(10L, tenant))
                .thenThrow(new TemplateNotFoundException("Template not found"));

        // Act & Assert
        assertThrows(
                TemplateNotFoundException.class,
                () -> notificationService.createNotification(request, tenant)
        );

        verify(templateService).getTemplateEntityById(10L, tenant);
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }


    @Test
    void shouldNotCreateNotificationWhenTemplateRenderingFails() {

        // Arrange
        Map<String, Object> variables = Map.of("name", "Rochona");

        Template template = new Template();
        template.setId(10L);
        template.setSubject("Hello {{name}}");
        template.setBodyText("Welcome {{name}}");
        template.setBodyHtml("<h1>Welcome {{name}}</h1>");

        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setTemplateId(10L);
        request.setVariables(variables);
        request.setRecipient("goon.rochona@gmail.com");
        request.setChannel(NotificationChannel.EMAIL);

        when(templateService.getTemplateEntityById(10L, tenant))
                .thenReturn(template);

        when(templateRenderer.renderText(template.getSubject(), variables))
                .thenThrow(new IllegalArgumentException("Missing template variable: name"));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.createNotification(request, tenant)
        );

        verify(templateService).getTemplateEntityById(10L, tenant);
        verify(templateRenderer).renderText(template.getSubject(), variables);

        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(outboxEventRepository);
    }


    @Test
    void shouldSuppressNotificationWhenChannelIsDisabled() {

        // Arrange
        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setSubject("Hi");
        request.setMessage("Hi there");
        request.setRecipient("test@gmail.com");
        request.setUserId("testuser101");
        request.setChannel(NotificationChannel.EMAIL);

        when(preferenceService.isChannelEnabled(
                tenant.getId(),
                "testuser101",
                NotificationChannel.EMAIL
        )).thenReturn(false);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    notification.setId(1L);
                    return notification;
                });

        // Act
        Notification result = notificationService.createNotification(request, tenant);

        // Assert
        assertNotNull(result);
        assertEquals(NotificationStatus.SUPPRESSED, result.getStatus());

        verify(preferenceService).isChannelEnabled(
                tenant.getId(),
                "testuser101",
                NotificationChannel.EMAIL
        );

        verify(notificationRepository).save(any(Notification.class));
        verifyNoInteractions(outboxEventRepository);
    }


    @Test
    void shouldSendNotificationWhenUserIdIsAbsent() {

        // Arrange
        NotificationRequestDTO request = new NotificationRequestDTO();
        request.setSubject("Hi");
        request.setMessage("Hello there");
        request.setRecipient("test@gmail.com");
        request.setChannel(NotificationChannel.EMAIL);
        // Deliberately leave userId null

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);

                    if (notification.getId() == null) {
                        notification.setId(1L);
                    }

                    return notification;
                });

        // Act
        Notification result = notificationService.createNotification(request, tenant);

        // Assert
        assertNotNull(result);
        assertEquals(NotificationStatus.PENDING, result.getStatus());
        assertNull(result.getUserId());

        verifyNoInteractions(preferenceService);
        verify(notificationRepository).save(any(Notification.class));
        verify(outboxEventRepository).save(argThat(event ->
                event.getNotification().getId().equals(result.getId())
                        && event.getEventType() == OutboxEventType.NOTIFICATION_CREATED
                        && event.getStatus() == OutboxEventStatus.PENDING
        ));
    }


    // ------------ getAllNotifications ------------->

    @Test
    void shouldReturnAllNotificationsForTenants(){

        Notification response1 = new Notification();
        response1.setId(1L);
        response1.setSubject("Test Subject 1");
        response1.setMessage("Test Message 1");
        response1.setRecipient("testrecipient@gmail.com");
        response1.setUserId("testuser101");
        response1.setChannel(NotificationChannel.EMAIL);
        response1.setStatus(NotificationStatus.PENDING);
        response1.setTenant(tenant);

        Notification response2 = new Notification();
        response2.setId(2L);
        response2.setSubject("Test Subject 2");
        response2.setMessage("Test Message 2");
        response2.setRecipient("testrecipient@gmail.com");
        response2.setUserId(null);
        response2.setChannel(NotificationChannel.EMAIL);
        response2.setStatus(NotificationStatus.PENDING);
        response2.setTenant(tenant);

        Pageable pageable = PageRequest.of(0, 20);

        Page<Notification> responsePage = new PageImpl<>(List.of(response1, response2), pageable, 2);


        when(notificationRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(responsePage);

        // Act

        Page<NotificationResponseDTO> result = notificationService.getAllNotifications(tenant, null, null, null, null, null, pageable);
        // Assert

        assertEquals(2, result.getContent().size());

        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("Test Subject 1", result.getContent().get(0).getSubject());
        assertEquals("testrecipient@gmail.com", result.getContent().get(0).getRecipient());
        assertEquals("Test Message 1", result.getContent().get(0).getMessage());
        assertEquals("testuser101", result.getContent().get(0).getUserId());
        assertEquals(NotificationChannel.EMAIL, result.getContent().get(0).getChannel());
        assertEquals(NotificationStatus.PENDING, result.getContent().get(0).getNotificationStatus());

        assertEquals(2L, result.getContent().get(1).getId());
        assertEquals("Test Subject 2", result.getContent().get(1).getSubject());
        assertEquals("testrecipient@gmail.com", result.getContent().get(1).getRecipient());
        assertEquals("Test Message 2", result.getContent().get(1).getMessage());
        assertNull(result.getContent().get(1).getUserId());
        assertEquals(NotificationChannel.EMAIL, result.getContent().get(1).getChannel());
        assertEquals(NotificationStatus.PENDING, result.getContent().get(1).getNotificationStatus());

        assertEquals(0, result.getNumber());
        assertEquals(20, result.getSize());
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());

        verify(notificationRepository).findAll(any(Specification.class), eq(pageable));

    }


    @Test
    void shouldReturnEmptyListWhenTenantHasNoNotifications(){

        Pageable pageable = PageRequest.of(0, 20);

        Page<Notification> responsePage = new PageImpl<>(List.of(), pageable, 0);

        // Arrange

        when(notificationRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(responsePage);

        // Act

        Page<NotificationResponseDTO> result = notificationService.getAllNotifications(tenant, null, null, null, null, null, pageable);

        // Assert

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(20, result.getSize());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());


        // verify the call
        verify(notificationRepository).findAll(any(Specification.class), eq(pageable));
    }



    // ------------- getNotificationById ------------->

    @Test
    void shouldReturnNotificationWhenIdAndTenantMatch(){

        // Arrange

        Notification response = new Notification();
        response.setId(1L);
        response.setSubject("Test Subject");
        response.setMessage("Test Message");
        response.setRecipient("goon.rochona@gmail.com");
        response.setUserId("testuser101");
        response.setChannel(NotificationChannel.EMAIL);
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
        assertEquals("testuser101", result.getUserId());
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
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



    // ------------- deleteNotificationById ----------->


    @Test // Need to make this idempotent
    void shouldDeleteAPendingNotificationSuccessfully(){

        // Arrange

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setUserId("testuser101");
        notification.setChannel(NotificationChannel.EMAIL);
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
        notification.setUserId("testuser101");
        notification.setChannel(NotificationChannel.EMAIL);
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


    @Test
    void shouldThrowExceptionWhenPendingNotificationCannotBeDeleted() {

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setTenant(tenant);

        when(notificationRepository.findByIdAndTenant(1L, tenant))
                .thenReturn(Optional.of(notification));

        when(notificationRepository.deleteByIdAndTenantAndStatus(
                1L, tenant, NotificationStatus.PENDING
        )).thenReturn(false);

        assertThrows(
                NotificationNotDeletableException.class,
                () -> notificationService.deleteNotificationById(1L, tenant)
        );

        verify(notificationRepository).findByIdAndTenant(1L, tenant);
        verify(notificationRepository).deleteByIdAndTenantAndStatus(
                1L, tenant, NotificationStatus.PENDING
        );
    }

    // ------------ Pagination & Filtering ---------------->
    @Test
    void shouldReturnSecondPageOfNotifications() {

        // Arrange

        Pageable pageable = PageRequest.of(1, 2);

        Notification notification = new Notification();
        notification.setId(3L);
        notification.setSubject("Test Subject 3");
        notification.setMessage("Test Message 3");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setTenant(tenant);

        Page<Notification> responsePage =
                new PageImpl<>(List.of(notification), pageable, 3);

        when(notificationRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(responsePage);

        // Act

        Page<NotificationResponseDTO> result =
                notificationService.getAllNotifications(tenant, null, null, null, null, null, pageable);


        // Assert

        assertEquals(1, result.getContent().size());
        assertEquals(3L, result.getContent().get(0).getId());
        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertFalse(result.hasNext());
        assertTrue(result.hasPrevious());


        // Verify
        verify(notificationRepository).findAll(any(Specification.class), eq(pageable));
    }


    @Test
    void shouldFilterNotificationsByStatus() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 20);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Failed notification");
        notification.setMessage("Test message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setTenant(tenant);

        Page<Notification> responsePage =
                new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(responsePage);

        // Act
        Page<NotificationResponseDTO> result =
                notificationService.getAllNotifications(
                        tenant, NotificationStatus.FAILED, null, null, null, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(
                NotificationStatus.FAILED,
                result.getContent().get(0).getNotificationStatus()
        );

        verify(notificationRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }


    @Test
    void shouldFilterNotificationsByChannel() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 20);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Failed notification");
        notification.setMessage("Test message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setTenant(tenant);

        Page<Notification> responsePage =
                new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(responsePage);

        // Act
        Page<NotificationResponseDTO> result =
                notificationService.getAllNotifications(
                        tenant, null, NotificationChannel.EMAIL, null, null, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(NotificationChannel.EMAIL, result.getContent().get(0).getChannel());

        verify(notificationRepository).findAll(any(Specification.class), eq(pageable));
    }



    @Test
    void shouldFilterNotificationsByUserId() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 20);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Failed notification");
        notification.setMessage("Test message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setTenant(tenant);
        notification.setUserId("testuser101");

        Page<Notification> responsePage =
                new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(responsePage);

        // Act
        Page<NotificationResponseDTO> result =
                notificationService.getAllNotifications(
                        tenant, null, null, "testuser101", null, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals("testuser101", result.getContent().get(0).getUserId());

        verify(notificationRepository).findAll(any(Specification.class), eq(pageable));
    }



}











