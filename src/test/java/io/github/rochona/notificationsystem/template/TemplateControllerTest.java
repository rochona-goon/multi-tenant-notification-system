package io.github.rochona.notificationsystem.template;

import io.github.rochona.notificationsystem.template.controller.TemplateController;
import io.github.rochona.notificationsystem.template.dto.TemplateRequestDto;
import io.github.rochona.notificationsystem.template.dto.TemplateResponseDto;
import io.github.rochona.notificationsystem.template.dto.TemplateStatusRequestDto;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyActiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyInactiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateNameAlreadyExistsException;
import io.github.rochona.notificationsystem.template.exception.TemplateNotFoundException;
import io.github.rochona.notificationsystem.template.service.TemplateService;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.service.TenantAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemplateController.class)
public class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TemplateService templateService;

    @MockitoBean
    private TenantAuthenticationService tenantAuthenticationService;

    private Tenant tenant;

    @BeforeEach
    void setUp(){
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");

        when(tenantAuthenticationService.authenticate(anyString()))
                .thenReturn(tenant);
    }

    // ------ create template ----->

    @Test
    void shouldReturn201WhenRequestIsValid() throws Exception{

        // Arrange

        TemplateResponseDto response = new TemplateResponseDto();
        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateService.createTemplate(any(TemplateRequestDto.class), any(Tenant.class)))
                .thenReturn(response);

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setSubject("Test Subject");
        requestDto.setName("Test Name");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");


        // Act & Assert

        mockMvc.perform(post("/templates/create")
                .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Name"))
                .andExpect(jsonPath("$.subject").value("Test Subject"))
                .andExpect(jsonPath("$.bodyText").value("Test Text Body"))
                .andExpect(jsonPath("$.bodyHtml").value("<h1> Test HTML Body </h1>"))
                .andExpect(jsonPath("$.active").value(true));

        ArgumentCaptor<TemplateRequestDto> requestCaptor = ArgumentCaptor.forClass(TemplateRequestDto.class);
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);

        verify(templateService).createTemplate(requestCaptor.capture(), tenantCaptor.capture());


        TemplateRequestDto capturedRequest = requestCaptor.getValue();
        Tenant capturedTenant = tenantCaptor.getValue();

        assertEquals("Test Name", capturedRequest.getName());
        assertEquals("Test Subject", capturedRequest.getSubject());
        assertEquals("Test Text Body", capturedRequest.getBodyText());
        assertEquals("<h1> Test HTML Body </h1>", capturedRequest.getBodyHtml());

        assertEquals(1L, capturedTenant.getId());
        assertEquals("Test Tenant", capturedTenant.getName());


    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception{

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setSubject("Test Subject");
        requestDto.setName(null);
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");


        // Act & Assert

        mockMvc.perform(post("/templates/create")
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());


        verify(templateService, never()).createTemplate(requestDto, tenant);
    }


    @Test
    void shouldReturn409WhenTemplateNameAlreadyExists() throws Exception{

        // Arrange

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setSubject("Test Subject");
        requestDto.setName("Test Name");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateService.createTemplate(any(TemplateRequestDto.class), any(Tenant.class)))
                .thenThrow(new TemplateNameAlreadyExistsException("Template name already exists"));

        // Act & Assert

        mockMvc.perform(post("/templates/create")
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());


        verify(templateService).createTemplate(any(TemplateRequestDto.class), any(Tenant.class));
    }


    // ----------- getAllTemplates -------->


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturn200WhenActiveOrInactiveTemplatesAreRequested(boolean active) throws Exception{

        // Arrange

        TemplateResponseDto response = new TemplateResponseDto();
        response.setId(1L);
        response.setActive(active);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");


        TemplateResponseDto response2 = new TemplateResponseDto();
        response2.setId(2L);
        response2.setActive(active);
        response2.setSubject("Test Subject2");
        response2.setName("Test Name2");
        response2.setBodyText("Test Text Body2");
        response2.setBodyHtml("<h1> Test HTML Body2 </h1>");

        when(templateService.getTemplates(eq(tenant), eq(active)))
                .thenReturn(List.of(response, response2));


        // Act & Assert

        MockHttpServletRequestBuilder request = get("/templates")
                .header("X-API-KEY", "tenant-api-key");

        if(!active){
            request.param("active", "false");
        }

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(active))
                .andExpect(jsonPath("$[1].active").value(active));


        verify(templateService).getTemplates(tenant, active);

    }

    @Test
    void shouldReturn200WithEmptyListWhenNoTemplatesExist() throws Exception{

        when(templateService.getTemplates(any(Tenant.class), anyBoolean()))
                .thenReturn(List.of());


        mockMvc.perform(get("/templates")
                .header("X-API-KEY", "tenant-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());


        verify(templateService).getTemplates(tenant, true);

    }


    // ---------- getTemplateById ---------->

    @Test
    void shouldReturn200WhenTemplateExists() throws Exception{

        // Arrange

        TemplateResponseDto response = new TemplateResponseDto();

        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateService.getTemplatesById(eq(1L), eq(tenant))).thenReturn(response);


        // Act & Assert

        mockMvc.perform(get("/templates/{id}", 1L).header("X-API-KEY", "tenant-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Name"))
                .andExpect(jsonPath("$.subject").value("Test Subject"))
                .andExpect(jsonPath("$.bodyText").value("Test Text Body"))
                .andExpect(jsonPath("$.bodyHtml").value("<h1> Test HTML Body </h1>"));

        // Verify the call
        verify(templateService).getTemplatesById(1L, tenant);
    }

    @Test
    void shouldReturn404WhenTemplateDoesNotExistForFetch() throws Exception{

        when(templateService.getTemplatesById(anyLong(), any(Tenant.class)))
                .thenThrow(new TemplateNotFoundException("Template does not exist"));

        mockMvc.perform(get("/templates/{id}",1L).header("X-API-KEY", "tenant-api-key"))
                .andExpect(status().isNotFound());

        verify(templateService).getTemplatesById(1L, tenant);

    }




    // -------- updateTemplateById ---------->

    @Test
    void shouldReturn200WhenTemplateIsUpdated() throws Exception{

        // Arrange

        TemplateResponseDto response = new TemplateResponseDto();
        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test2 Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setSubject("Test Subject");
        requestDto.setName("Test2 Name");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateService.updateTemplateById(eq(1L), any(TemplateRequestDto.class), same(tenant))).thenReturn(response);


        // Act & Assert

        mockMvc.perform(put("/templates/{id}", 1L)
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test2 Name"))
                .andExpect(jsonPath("$.subject").value("Test Subject"))
                .andExpect(jsonPath("$.bodyText").value("Test Text Body"))
                .andExpect(jsonPath("$.bodyHtml").value("<h1> Test HTML Body </h1>"))
                .andExpect(jsonPath("$.active").value(true));



        verify(templateService).updateTemplateById(eq(1L), any(TemplateRequestDto.class), same(tenant));

    }

    @Test
    void shouldReturn400WhenUpdateRequestIsInvalid() throws Exception{

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setSubject("Test Subject");
        requestDto.setName(null);
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");


        mockMvc.perform(put("/templates/{id}", 1L)
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(templateService, never()).updateTemplateById(eq(1L), any(TemplateRequestDto.class), same(tenant));

    }


    @Test
    void shouldReturn404WhenUpdatingNonExistentTemplate() throws Exception{

        // Arrange

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setSubject("Test Subject");
        requestDto.setName("Test name");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateService.updateTemplateById(eq(1L), any(TemplateRequestDto.class), eq(tenant)))
                .thenThrow(new TemplateNotFoundException("Template does not exist"));


        // Act & Assert

        mockMvc.perform(put("/templates/{id}",1L)
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        // Verify the call
        verify(templateService).updateTemplateById( eq(1L), any(TemplateRequestDto.class), same(tenant));

    }

    @Test
    void shouldReturn409WhenUpdatingWithDuplicateName() throws Exception{

        // Arrange

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setSubject("Test Subject");
        requestDto.setName("Test name");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateService.updateTemplateById(eq(1L), any(TemplateRequestDto.class), eq(tenant)))
                .thenThrow(new TemplateNameAlreadyExistsException("Template name already exists"));


        // Act & Assert

        MockHttpServletRequestBuilder request = put("/templates/{id}",1L)
                .header("X-API-KEY", "tenant-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto));

        mockMvc.perform(request)
                .andExpect(status().isConflict());

        // Verify the call
        verify(templateService).updateTemplateById( eq(1L), any(TemplateRequestDto.class), same(tenant));

    }



    // --------- updateTemplateStatus ---------->

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturn200WhenTemplateStatusDeactivatedOrReactivated(boolean active) throws Exception{

        // Arrange

        TemplateResponseDto response = new TemplateResponseDto();
        response.setId(1L);
        response.setActive(active);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        TemplateStatusRequestDto statusDto = new TemplateStatusRequestDto();
        statusDto.setActive(active);

        when(templateService.updateTemplateStatus(eq(1L), eq(active), same(tenant)))
                .thenReturn(response);


        // Act & Assert

        mockMvc.perform(patch("/templates/{id}/status", 1L)
                .header("X-API-KEY", "tenant-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(active));


        // Verify the call
        verify(templateService).updateTemplateStatus(eq(1L), eq(active), same(tenant));

    }


    @Test
    void shouldReturn400WhenStatusIsMissing() throws Exception{

        // Arrange

        TemplateStatusRequestDto statusDto = new TemplateStatusRequestDto();
        statusDto.setActive(null);

        // Act & Assert

        mockMvc.perform(patch("/templates/{id}/status", 1L)
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isBadRequest());

        // Verify the call
        verify(templateService, never()).updateTemplateStatus(eq(1L), anyBoolean(), same(tenant));
    }


    @Test
    void shouldReturn404WhenTemplateDoesNotExistForStatus() throws Exception{

        // Arrange

        TemplateStatusRequestDto statusDto = new TemplateStatusRequestDto();
        statusDto.setActive(true);

        when(templateService.updateTemplateStatus(eq(1L), anyBoolean(), same(tenant)))
                .thenThrow(new TemplateNotFoundException("Template not found"));


        // Act & Assert
        mockMvc.perform(patch("/templates/{id}/status", 1L)
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isNotFound());

        // Verify the call
        verify(templateService).updateTemplateStatus(eq(1L), anyBoolean(), same(tenant));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturn409WhenTemplateIsAlreadyActiveOrDeactive(boolean active) throws Exception{

        // Arrange

        TemplateStatusRequestDto statusDto = new TemplateStatusRequestDto();
        statusDto.setActive(active);

        if (active) {
            when(templateService.updateTemplateStatus(eq(1L), eq(true), same(tenant)))
                    .thenThrow(new TemplateAlreadyActiveException("Template is already active"));
        } else {
            when(templateService.updateTemplateStatus(eq(1L), eq(false), same(tenant)))
                    .thenThrow(new TemplateAlreadyInactiveException("Template is already inactive"));
        }


        // Act & Assert
        mockMvc.perform(patch("/templates/{id}/status", 1L)
                        .header("X-API-KEY", "tenant-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isConflict());

        // Verify the call
        verify(templateService).updateTemplateStatus(eq(1L), anyBoolean(), same(tenant));

    }

}
