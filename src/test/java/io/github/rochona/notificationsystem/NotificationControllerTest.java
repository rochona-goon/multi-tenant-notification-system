package io.github.rochona.notificationsystem;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.service.NotificationService;
import io.github.rochona.notificationsystem.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

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
                anyString(), anyString(), anyString()
        )).thenReturn(notification);

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage("Test Message");
        requestDTO.setRecipient("testrecipient@gmail.com");


        // Act

        mockMvc.perform(post("/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
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

        mockMvc.perform(post("/notify")
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

        mockMvc.perform(post("/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }
}
