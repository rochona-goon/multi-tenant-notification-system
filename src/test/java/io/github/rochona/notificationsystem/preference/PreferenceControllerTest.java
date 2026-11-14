package io.github.rochona.notificationsystem.preference;

import io.github.rochona.notificationsystem.preference.controller.PreferenceController;
import io.github.rochona.notificationsystem.preference.dto.PreferenceRequestDto;
import io.github.rochona.notificationsystem.preference.dto.PreferenceResponseDto;
import io.github.rochona.notificationsystem.preference.exception.PreferenceDoesNotExistException;
import io.github.rochona.notificationsystem.preference.service.PreferenceService;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.service.TenantAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreferenceController.class)
public class PreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PreferenceService preferenceService;

    @MockitoBean
    private TenantAuthenticationService tenantAuthenticationService;

    private Tenant tenant;

    private static Stream<String> invalidPreferenceRequests() {
        return Stream.of(
                """
                {
                    "smsEnabled": true,
                    "pushEnabled": true
                }
                """,
                """
                {
                    "emailEnabled": true,
                    "pushEnabled": true
                }
                """,
                """
                {
                    "emailEnabled": true,
                    "smsEnabled": true
                }
                """
        );
    }

    @BeforeEach
    void setUp(){
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");

        when(tenantAuthenticationService.authenticate(anyString()))
                .thenReturn(tenant);
    }


    // --------- PUT /preferences/{userId} – Create or update preferences --------->

    @Test
    void shouldReturn200WhenPreferenceUpserted() throws Exception{
        // Arrange

        PreferenceResponseDto response = new PreferenceResponseDto();
        response.setEmailEnabled(true);
        response.setSmsEnabled(false);
        response.setPushEnabled(true);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        PreferenceRequestDto request = new PreferenceRequestDto();
        request.setEmailEnabled(true);
        request.setSmsEnabled(false);
        request.setPushEnabled(true);

        when(preferenceService.upsertPreference(eq(tenant), eq("testuser101"), any(PreferenceRequestDto.class)))
                .thenReturn(response);


        // Act & Assert

        mockMvc.perform(put("/preferences/testuser101")
                .header("X-API-KEY", "tenant-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailEnabled").value(true))
            .andExpect(jsonPath("$.smsEnabled").value(false))
            .andExpect(jsonPath("$.pushEnabled").value(true));



        // Verify the call

        verify(preferenceService).upsertPreference(
                eq(tenant),
                eq("testuser101"),
                any(PreferenceRequestDto.class));

    }

    // Email/SMS/PUSH
    @ParameterizedTest
    @MethodSource("invalidPreferenceRequests")
    void shouldReturn400BadRequestWhenChannelEnabledIsMissing(String requestBody) throws Exception{

        mockMvc.perform(put("/preferences/testuser101")
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferenceService);
    }


    @Test
    void shouldReturn400BadRequestWhenUserIdIsBlank() throws Exception{

        // Arrange

        PreferenceRequestDto request = new PreferenceRequestDto();
        request.setEmailEnabled(true);
        request.setSmsEnabled(false);
        request.setPushEnabled(true);

        // Act & Assert

        mockMvc.perform(put("/preferences/ ")
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferenceService);

    }


    @Test
    void shouldReturn400BadRequestWhenUserIdExceeds100Characters() throws Exception{

        // Arrange

        String userId = "a".repeat(101);

        PreferenceRequestDto request = new PreferenceRequestDto();
        request.setEmailEnabled(true);
        request.setSmsEnabled(false);
        request.setPushEnabled(true);


        // Act & Assert

        mockMvc.perform(put("/preferences/{userId}", userId)
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferenceService);

    }



    // ----------- GET /preferences/{userId} – Retrieve one preference ----------->

    @Test
    void shouldGetPreferenceSuccessfully() throws Exception{

        // Arrange

        PreferenceResponseDto response = new PreferenceResponseDto();
        response.setEmailEnabled(true);
        response.setSmsEnabled(false);
        response.setPushEnabled(true);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        when(preferenceService.getPreference(eq(tenant.getId()), eq("testuser101")))
                .thenReturn(response);


        // Act & Assert

        mockMvc.perform(get("/preferences/testuser101")
                        .header("X-API-KEY", "tenant-api-key"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("testuser101"))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.smsEnabled").value(false))
                .andExpect(jsonPath("$.pushEnabled").value(true));


        // Verify
        verify(preferenceService).getPreference(eq(tenant.getId()), eq("testuser101"));

    }

    @Test
    void shouldReturn404NotFoundWhenPreferenceDoesNotExist() throws Exception{

        // Arrange

        PreferenceResponseDto response = new PreferenceResponseDto();
        response.setEmailEnabled(true);
        response.setSmsEnabled(false);
        response.setPushEnabled(true);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        when(preferenceService.getPreference(eq(tenant.getId()), eq("testuser101")))
                .thenThrow(new PreferenceDoesNotExistException("Preference does not exist"));


        // Act & Assert

        mockMvc.perform(get("/preferences/testuser101")
                        .header("X-API-KEY", "tenant-api-key"))
                .andDo(print())
                .andExpect(status().isNotFound());

        // Verify
        verify(preferenceService).getPreference(eq(tenant.getId()), eq("testuser101"));

    }

    @Test
    void shouldReturn400BadRequestWhenUserIdIsInvalid() throws Exception{

        // Arrange

        String userId = "a".repeat(101);


        // Act & Assert

        mockMvc.perform(get("/preferences/{userId}", userId)
                        .header("X-API-KEY", "tenant-api-key"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify
        verify(preferenceService, never()).getPreference(eq(tenant.getId()), eq("testuser101"));


    }



    // --------------- GET /preferences – Retrieve all preferences ---------------->

    @Test
    void shouldGetAllPreferencesSuccessfully() throws Exception{

        // Arrange

        PreferenceResponseDto response = new PreferenceResponseDto();
        response.setEmailEnabled(true);
        response.setSmsEnabled(false);
        response.setPushEnabled(true);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());


        PreferenceResponseDto response1 = new PreferenceResponseDto();
        response1.setEmailEnabled(true);
        response1.setSmsEnabled(true);
        response1.setPushEnabled(true);
        response1.setUserId("testuser102");
        response1.setCreatedAt(Instant.now());
        response1.setUpdatedAt(Instant.now());

        when(preferenceService.getAllPreferences(eq(tenant.getId())))
                .thenReturn(List.of(response,response1));


        // Act & Assert

        mockMvc.perform(get("/preferences")
                        .header("X-API-KEY", "tenant-api-key"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("testuser101"))
                .andExpect(jsonPath("$[0].emailEnabled").value(true))
                .andExpect(jsonPath("$[0].smsEnabled").value(false))
                .andExpect(jsonPath("$[0].pushEnabled").value(true))
                .andExpect(jsonPath("$[1].userId").value("testuser102"))
                .andExpect(jsonPath("$[1].emailEnabled").value(true))
                .andExpect(jsonPath("$[1].smsEnabled").value(true))
                .andExpect(jsonPath("$[1].pushEnabled").value(true));


        // Verify
        verify(preferenceService).getAllPreferences(eq(tenant.getId()));

    }


    @Test
    void shouldReturnEmptyListWhenNoPreferencesExist() throws Exception{

        when(preferenceService.getAllPreferences(eq(tenant.getId())))
                .thenReturn(List.of());


        // Act & Assert

        mockMvc.perform(get("/preferences")
                        .header("X-API-KEY", "tenant-api-key"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

    }



    // ------------- DELETE /preferences/{userId} – Delete preference ------------>


    @Test
    void shouldDeletePreferenceSuccessfully() throws Exception{

        // Act & Assert

        mockMvc.perform(delete("/preferences/testuser101")
                        .header("X-API-KEY", "tenant-api-key"))
                .andDo(print())
                .andExpect(status().isNoContent());


        verify(preferenceService).deletePreference(eq(tenant.getId()), eq("testuser101"));


    }

    @Test
    void shouldReturn204NoContentWhenDeletingNonexistentPreference() throws Exception{

        mockMvc.perform(delete("/preferences/testuser101")
                        .header("X-API-KEY", "tenant-api-key"))
                .andExpect(status().isNoContent());

        verify(preferenceService).deletePreference(
                tenant.getId(), "testuser101");
    }

    @Test
    void shouldReturn400BadRequestWhenUserIdIsInvalidForDeletion() throws Exception{

        // Arrange

        String userId = "a".repeat(101);


        // Act & Assert

        mockMvc.perform(delete("/preferences/{userId}", userId)
                        .header("X-API-KEY", "tenant-api-key"))
                .andDo(print())
                .andExpect(status().isBadRequest());


        // Verify

        verify(preferenceService, never()).deletePreference(eq(tenant.getId()), eq("testuser101"));
    }

}
