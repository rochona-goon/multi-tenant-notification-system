package io.github.rochona.notificationsystem.tenant;

import io.github.rochona.notificationsystem.tenant.controller.TenantController;
import io.github.rochona.notificationsystem.tenant.dto.TenantRegistrationRequest;
import io.github.rochona.notificationsystem.tenant.dto.TenantRegistrationResponse;
import io.github.rochona.notificationsystem.tenant.service.TenantAuthenticationService;
import io.github.rochona.notificationsystem.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantController.class)
public class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantAuthenticationService tenantAuthenticationService;


    @Test
    void shouldReturn201ForSuccessfulTenantRegistration() throws Exception{

        // Arrange

        TenantRegistrationRequest requestDto = new TenantRegistrationRequest();
        requestDto.setTenantName("Test Tenant");

        String rawApiKey = "ntf_live_api-key";

        TenantRegistrationResponse response = new TenantRegistrationResponse();
        response.setTenantId(1L);
        response.setTenantName("Test Tenant");
        response.setApiKey(rawApiKey);

        when(tenantService.registerTenant(anyString()))
                .thenReturn(response);


        // Act & Assert

        mockMvc.perform(post("/tenants/signUp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantName").value("Test Tenant"))
                .andExpect(jsonPath("$.tenantId").value(1L))
                .andExpect(jsonPath("$.apiKey").value(rawApiKey));



        // Verify the call
        verify(tenantService).registerTenant("Test Tenant");


    }


    @Test
    void shouldReturn400WhenTenantNameIsBlank() throws Exception{

        // Arrange

        TenantRegistrationRequest requestDto = new TenantRegistrationRequest();
        requestDto.setTenantName(" ");


        // Act & Assert

        mockMvc.perform(post("/tenants/signUp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());


        // Verify the call
        verify(tenantService, never()).registerTenant(" ");

    }
}
