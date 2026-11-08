package io.github.rochona.notificationsystem.notification;

import io.github.rochona.notificationsystem.notification.controller.NotificationController;
import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.service.NotificationService;
import io.github.rochona.notificationsystem.tenant.exception.InvalidApiKeyException;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.service.TenantAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private TenantAuthenticationService tenantAuthenticationService;

    @BeforeEach
    void setUp(){
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");

        when(tenantAuthenticationService.authenticate(any()))
                .thenReturn(tenant);
    }

    @Test
    void shouldReturn201WhenRequestIsValid() throws Exception {
        // Arrange

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setStatus(NotificationStatus.PENDING);

        when(notificationService.createNotification(
                anyString(), anyString(), anyString(), any(Tenant.class)
        )).thenReturn(notification);

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage("Test Message");
        requestDTO.setRecipient("testrecipient@gmail.com");


        // Act

        mockMvc.perform(post("/notifications/create")
                        .header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.recipient").value("testrecipient@gmail.com"))
                .andExpect(jsonPath("$.notificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"));


    }

    @Test
    void shouldReturn400WhenMessageIsBlank() throws Exception {
        // Arrange

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage(" ");
        requestDTO.setRecipient("testrecipient@gmail.com");


        // Act & Assert

        mockMvc.perform(post("/notifications/create")
                        .header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRecipientIsBlank() throws Exception {
        // Arrange

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage("Test Message");
        requestDTO.setRecipient(" ");


        // Act & Assert

        mockMvc.perform(post("/notifications/create")
                        .header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn401ForMissingApiKey() throws Exception{

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage("Test Message");
        requestDTO.setRecipient("testRecipient@gmail.com");

        when(tenantAuthenticationService.authenticate(isNull()))
                .thenThrow(new InvalidApiKeyException("API key is missing"));

        mockMvc.perform(post("/notifications/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldReturn401ForBlankApiKey() throws Exception{

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage("Test Message");
        requestDTO.setRecipient("testRecipient@gmail.com");

        when(tenantAuthenticationService.authenticate(" "))
                .thenThrow(new InvalidApiKeyException("API key is blank"));

        mockMvc.perform(post("/notifications/create")
                        .header("X-API-Key", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturn200WhenFetchingAllNotifications() throws Exception {
        // Arrange

        NotificationResponseDTO notification1 = new NotificationResponseDTO();
        notification1.setId(1L);
        notification1.setSubject("Test Subject 1");
        notification1.setMessage("Test Message 1");
        notification1.setRecipient("testrecipient1@gmail.com");
        notification1.setNotificationStatus(NotificationStatus.PENDING);

        NotificationResponseDTO notification2 = new NotificationResponseDTO();
        notification2.setId(2L);
        notification2.setSubject("Test Subject 2");
        notification2.setMessage("Test Message 2");
        notification2.setRecipient("testrecipient2@gmail.com");
        notification2.setNotificationStatus(NotificationStatus.PENDING);

        when(notificationService.getAllNotifications(any(Tenant.class)))
                .thenReturn(List.of(notification1, notification2));


        // Act & Assert

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].subject").value("Test Subject 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].subject").value("Test Subject 2"));

    }

    @Test
    void shouldReturn200WhenFetchingNotificationById() throws Exception{

        // Arrange

        NotificationResponseDTO responseDTO = new NotificationResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setSubject("Test Subject");
        responseDTO.setMessage("Test Message");
        responseDTO.setRecipient("testrecipient@gmail.com");
        responseDTO.setNotificationStatus(NotificationStatus.PENDING);

        when(notificationService.getNotificationById(eq(1L), any(Tenant.class)))
                .thenReturn(responseDTO);

        // Act & Assert

        mockMvc.perform(get("/notifications/{id}",  1L)
                        .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.subject").value("Test Subject"))
                .andExpect(jsonPath("$.message").value("Test Message"))
                .andExpect(jsonPath("$.recipient").value("testrecipient@gmail.com"))
                .andExpect(jsonPath("$.notificationStatus").value("PENDING"));

    }



    @Test
    void shouldReturn404WhenNotificationIsNotFound()  throws Exception{

        // Arrange

        when(notificationService.getNotificationById(eq(999L), any(Tenant.class)))
                .thenThrow(new NotificationNotFoundException("Notification not found"));

        // Act & Assert

        mockMvc.perform(get("/notifications/{id}", 999L)
                        .header("X-API-Key", "test-api-key"))
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturn204WhenPendingNotificationIsDeleted() throws Exception{

        // Arrange

        when(notificationService.deleteNotificationById(eq(1L), any(Tenant.class)))
                .thenReturn(true);

        // Act & Assert

        mockMvc.perform(delete("/notifications/{id}", 1L)
                        .header("X-API-Key", "test-api-key"))
                .andExpect(status().isNoContent());
    }


    @Test
    void shouldReturn409whenNonPendingNotificationIsDeleted() throws Exception {

        // Arrange

        when(notificationService.deleteNotificationById(eq(1L), any(Tenant.class)))
                .thenThrow(new NotificationNotDeletableException("Only PENDING Notifications can be deleted"));


        // Act & Assert

        mockMvc.perform(delete("/notifications/{id}", 1L)
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isConflict());
    }

}
