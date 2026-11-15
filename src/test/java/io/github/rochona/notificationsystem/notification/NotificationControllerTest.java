package io.github.rochona.notificationsystem.notification;

import io.github.rochona.notificationsystem.notification.controller.NotificationController;
import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.dto.NotificationResponseDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotDeletableException;
import io.github.rochona.notificationsystem.notification.exception.NotificationNotFoundException;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.service.NotificationService;
import io.github.rochona.notificationsystem.ratelimit.RateLimiterService;
import io.github.rochona.notificationsystem.tenant.exception.InvalidApiKeyException;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.service.TenantAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private TenantAuthenticationService tenantAuthenticationService;

    @BeforeEach
    void setUp(){
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");

        when(tenantAuthenticationService.authenticate(any()))
                .thenReturn(tenant);

        when(rateLimiterService.isAllowed(anyLong())).thenReturn(true);

    }

    @Test
    void shouldReturn201WhenRequestIsValid() throws Exception {
        // Arrange

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setRecipient("testrecipient@gmail.com");
        notification.setUserId("testuser101");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);

        when(notificationService.createNotification(
                any(NotificationRequestDTO.class),
                any(Tenant.class)
        )).thenReturn(notification);

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage("Test Message");
        requestDTO.setRecipient("testrecipient@gmail.com");
        requestDTO.setUserId("testuser101");
        requestDTO.setChannel(NotificationChannel.EMAIL);


        // Act

        mockMvc.perform(post("/notifications/create")
                        .header("X-API-Key", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.recipient").value("testrecipient@gmail.com"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.userId").value("testuser101"))
                .andExpect(jsonPath("$.notificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.subject").value("Test Subject"))
                .andExpect(jsonPath("$.message").value("Test Message"))
                .andExpect(jsonPath("$.htmlMessage").doesNotExist());


        // Verify
        verify(notificationService).createNotification(
                any(NotificationRequestDTO.class),
                any(Tenant.class)
        );
    }

    @Test
    void shouldReturn400WhenMessageIsBlank() throws Exception {
        // Arrange

        NotificationRequestDTO requestDTO = new NotificationRequestDTO();
        requestDTO.setSubject("Test Subject");
        requestDTO.setMessage(" ");
        requestDTO.setRecipient("testrecipient@gmail.com");
        requestDTO.setUserId("testuser101");
        requestDTO.setChannel(NotificationChannel.EMAIL);


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
        requestDTO.setUserId("testuser101");
        requestDTO.setChannel(NotificationChannel.EMAIL);


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
        requestDTO.setUserId("testuser101");
        requestDTO.setChannel(NotificationChannel.EMAIL);

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
        requestDTO.setUserId("testuser101");
        requestDTO.setChannel(NotificationChannel.EMAIL);

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
        notification1.setUserId("testuser101");
        notification1.setChannel(NotificationChannel.EMAIL);

        NotificationResponseDTO notification2 = new NotificationResponseDTO();
        notification2.setId(2L);
        notification2.setSubject("Test Subject 2");
        notification2.setMessage("Test Message 2");
        notification2.setRecipient("testrecipient2@gmail.com");
        notification2.setNotificationStatus(NotificationStatus.PENDING);
        notification2.setUserId("testuser102");
        notification2.setChannel(NotificationChannel.EMAIL);

        Pageable pageable = PageRequest.of(0, 20);

        Page<NotificationResponseDTO> responsePage = new PageImpl<>(List.of(notification1, notification2), pageable, 2);

        when(notificationService.getAllNotifications(any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                any(Pageable.class)))
                .thenReturn(responsePage);


        // Act & Assert

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].subject").value("Test Subject 1"))
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].subject").value("Test Subject 2"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].notificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.content[1].notificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[1].channel").value("EMAIL"));


        // Verify

        verify(notificationService).getAllNotifications(any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                eq(PageRequest.of(0, 20, Sort.by("createdAt").descending())));

    }

    @Test
    void shouldReturn200WhenFetchingNotificationById() throws Exception{

        // Arrange

        NotificationResponseDTO responseDTO = new NotificationResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setSubject("Test Subject");
        responseDTO.setMessage("Test Message");
        responseDTO.setRecipient("testrecipient@gmail.com");
        responseDTO.setUserId("testuser101");
        responseDTO.setChannel(NotificationChannel.EMAIL);
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
                .andExpect(jsonPath("$.userId").value("testuser101"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
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


    // ---------------- Pagination & Filtering ----------------->

    @Test
    void shouldReturn200WhenFetchingNotificationsWithCustomPagination() throws Exception {

        // Act & Assert

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        // Verify

        verify(notificationService).getAllNotifications(any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                eq(PageRequest.of(1, 5, Sort.by("createdAt").descending())));
    }


    @Test
    void shouldReturn400WhenPageNumberIsNegative() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(notificationService, never())
                .getAllNotifications(any(Tenant.class),
                        nullable(NotificationStatus.class),
                        nullable(NotificationChannel.class),
                        nullable(String.class),
                        nullable(LocalDate.class),
                        nullable(LocalDate.class),
                        any(Pageable.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101"})
    void shouldReturn400WhenPageSizeIsInvalid(String size) throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("size", size))
                .andExpect(status().isBadRequest());

        verify(notificationService, never())
                .getAllNotifications(any(Tenant.class),
                        nullable(NotificationStatus.class),
                        nullable(NotificationChannel.class),
                        nullable(String.class),
                        nullable(LocalDate.class),
                        nullable(LocalDate.class),
                        any(Pageable.class));
    }


    @Test
    void shouldAcceptMaximumPageSize() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("size", "100"))
                .andExpect(status().isOk());

        verify(notificationService).getAllNotifications(
                any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                eq(PageRequest.of(
                        0,
                        100,
                        Sort.by("createdAt").descending()
                ))
        );
    }



    @Test
    void shouldReturnNotificationsFilteredByStatus() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("status", "FAILED"))
                .andExpect(status().isOk());

        verify(notificationService).getAllNotifications(
                any(Tenant.class),
                eq(NotificationStatus.FAILED),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                eq(PageRequest.of(
                        0, 20,
                        Sort.by("createdAt").descending()
                ))
        );
    }


    @Test
    void shouldReturnNotificationsFilteredByChannel() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("channel", "EMAIL"))
                .andExpect(status().isOk());

        verify(notificationService).getAllNotifications(
                any(Tenant.class),
                nullable(NotificationStatus.class),
                eq(NotificationChannel.EMAIL),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                eq(PageRequest.of(
                        0, 20,
                        Sort.by("createdAt").descending()
                ))
        );
    }


    @Test
    void shouldReturnNotificationsFilteredByUserId() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("userId", "testuser101"))
                .andExpect(status().isOk());

        verify(notificationService).getAllNotifications(
                any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                eq("testuser101"),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                eq(PageRequest.of(
                        0, 20,
                        Sort.by("createdAt").descending()
                ))
        );
    }


    @Test
    void shouldReturnNotificationsFilteredByMultipleCriteria() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("status", "SENT")
                        .param("channel", "EMAIL")
                        .param("userId", "testuser101")
                        .param("fromDate", "2026-11-01")
                        .param("toDate", "2026-11-13"))
                .andExpect(status().isOk());

        verify(notificationService).getAllNotifications(
                any(Tenant.class),
                eq(NotificationStatus.SENT),
                eq(NotificationChannel.EMAIL),
                eq("testuser101"),
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 11, 13)),
                eq(PageRequest.of(
                        0, 20,
                        Sort.by("createdAt").descending()
                ))
        );
    }


    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getAllNotifications(
                any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                any(Pageable.class)
        );
    }


    @Test
    void shouldReturn400WhenChannelIsInvalid() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("channel", "INVALID_CHANNEL"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getAllNotifications(
                any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                any(Pageable.class)
        );
    }


    @Test
    void shouldReturn400WhenFromDateIsAfterToDate() throws Exception {

        mockMvc.perform(get("/notifications")
                        .header("X-API-Key", "test-api-key")
                        .param("fromDate", "2026-11-13")
                        .param("toDate", "2026-11-01"))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getAllNotifications(
                any(Tenant.class),
                nullable(NotificationStatus.class),
                nullable(NotificationChannel.class),
                nullable(String.class),
                nullable(LocalDate.class),
                nullable(LocalDate.class),
                any(Pageable.class)
        );
    }
}
