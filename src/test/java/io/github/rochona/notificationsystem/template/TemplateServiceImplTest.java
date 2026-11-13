package io.github.rochona.notificationsystem.template;

import io.github.rochona.notificationsystem.template.dto.TemplateRequestDto;
import io.github.rochona.notificationsystem.template.dto.TemplateResponseDto;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyActiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyInactiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateNameAlreadyExistsException;
import io.github.rochona.notificationsystem.template.exception.TemplateNotFoundException;
import io.github.rochona.notificationsystem.template.model.Template;
import io.github.rochona.notificationsystem.template.repository.TemplateRepository;
import io.github.rochona.notificationsystem.template.service.TemplateServiceImpl;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private Tenant tenant;

    @BeforeEach
    void setUp(){
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");
    }

    // ---------- createTemplate ---------->

    @Test
    void shouldCreateTemplateSuccessfully(){

        // Arrange

        Template response = new Template();
        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");


        when(templateRepository.saveAndFlush(any(Template.class)))
                .thenReturn(response);

        // Act

        TemplateResponseDto result = templateService.createTemplate(requestDto, tenant);

        // Assert

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("Test Name", result.getName());
        assertEquals("Test Text Body", result.getBodyText());
        assertEquals("<h1> Test HTML Body </h1>", result.getBodyHtml());


        // Verify the call
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);

        verify(templateRepository).saveAndFlush(templateCaptor.capture());


        // Assert

        Template savedTemplate = templateCaptor.getValue();

        assertEquals("Test Name", savedTemplate.getName());
        assertEquals("Test Subject", savedTemplate.getSubject());
        assertEquals("Test Text Body", savedTemplate.getBodyText());
        assertEquals("<h1> Test HTML Body </h1>", savedTemplate.getBodyHtml());
        assertEquals(tenant, savedTemplate.getTenant());
    }

    @Test
    void shouldThrowExceptionWhenTemplateNameAlreadyExists(){
        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateRepository.existsByTenantIdAndName(eq(tenant.getId()), anyString()))
                .thenReturn(true);

        // Act & Assert

        assertThrows(
                TemplateNameAlreadyExistsException.class,
                () -> templateService.createTemplate(requestDto, tenant)
        );

        // Verify the call
        verify(templateRepository).existsByTenantIdAndName(eq(tenant.getId()), anyString());
        verify(templateRepository, never()).saveAndFlush(any(Template.class));

    }

    @Test
    void shouldThrowExceptionWhenConcurrentDuplicateTemplateIsCreated(){

        // Arrange

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        ConstraintViolationException constraintException = new ConstraintViolationException(
                "Duplicate template name", null, "uk_template_tenant_name");

        DataIntegrityViolationException databaseException = new DataIntegrityViolationException(
                "Database constraint violation", constraintException);


        when(templateRepository.existsByTenantIdAndName(eq(tenant.getId()), anyString()))
                .thenReturn(false);

        when(templateRepository.saveAndFlush(any(Template.class)))
                .thenThrow(databaseException);


        // Act & Assert

        assertThrows(TemplateNameAlreadyExistsException.class,
                () -> templateService.createTemplate(requestDto, tenant));


        // Verify the call

        verify(templateRepository).existsByTenantIdAndName(eq(tenant.getId()), anyString());
        verify(templateRepository).saveAndFlush(any(Template.class));

    }


    @Test
    void shouldRethrowDataIntegrityViolationForDifferentConstraint(){

        // Arrange

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        ConstraintViolationException constraintException = new ConstraintViolationException(
            "Some other database violation", null, "some_other_constraint");


        DataIntegrityViolationException databaseException = new DataIntegrityViolationException(
                "Database constraint violation", constraintException);


        when(templateRepository.existsByTenantIdAndName(eq(tenant.getId()), anyString()))
                .thenReturn(false);

        when(templateRepository.saveAndFlush(any(Template.class)))
                .thenThrow(databaseException);


        // Act & Assert

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> templateService.createTemplate(requestDto, tenant));

        assertEquals(databaseException, exception);


        // Verify the call

        verify(templateRepository).existsByTenantIdAndName(eq(tenant.getId()), anyString());
        verify(templateRepository).saveAndFlush(any(Template.class));

    }



    // ------------ getTemplates ----------->

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnTemplatesBasedOnActiveStatus(boolean active) {
        // Arrange
        // Create sample templates with the specified active status.
        Template response = new Template();
        response.setId(1L);
        response.setActive(active);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");


        Template response2 = new Template();
        response2.setId(2L);
        response2.setActive(active);
        response2.setSubject("Test Subject2");
        response2.setName("Test Name2");
        response2.setBodyText("Test Text Body2");
        response2.setBodyHtml("<h1> Test HTML Body2 </h1>");


        when(templateRepository.findAllByTenantIdAndIsActive(eq(tenant.getId()), eq(active)))
                .thenReturn(List.of(response, response2));

        // Act

        List<TemplateResponseDto> result = templateService.getTemplates(tenant, active);


        // Assert

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).getId());
        assertEquals("Test Name", result.get(0).getName());
        assertEquals(active, result.get(0).isActive());

        assertEquals(2L, result.get(1).getId());
        assertEquals("Test Name2", result.get(1).getName());
        assertEquals(active, result.get(1).isActive());


        // Verify repository invocation
        verify(templateRepository).findAllByTenantIdAndIsActive(eq(tenant.getId()), eq(active));
    }


    @Test
    void shouldReturnEmptyListWhenNoTemplatesExist(){

        // Arrange

        when(templateRepository.findAllByTenantIdAndIsActive(eq(tenant.getId()), anyBoolean()))
                .thenReturn(List.of());

        // Act

        List<TemplateResponseDto> result = templateService.getTemplates(tenant, true);

        // Assert

        assertTrue(result.isEmpty());

        // Verify the call
        verify(templateRepository).findAllByTenantIdAndIsActive(eq(tenant.getId()), eq(true));

    }



    // ------------ getTemplatesById ------------>

    @Test
    void shouldReturnTemplateWhenItExists(){

        // Arrange

        Template response = new Template();
        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.of(response));

        // Act

        TemplateResponseDto result = templateService.getTemplatesById(1L, tenant);

        // Assert

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("Test Name", result.getName());
        assertEquals("Test Text Body", result.getBodyText());
        assertEquals("<h1> Test HTML Body </h1>", result.getBodyHtml());

        // Verify the call
        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
    }

    @Test
    void shouldThrowExceptionWhenTemplateDoesNotExist(){

        // Arrange

        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.empty());

        // Act & Assert

        assertThrows(
                TemplateNotFoundException.class,
                () -> templateService.getTemplatesById(1L, tenant)
        );


        // Verify the call
        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
    }

    @Test
    void shouldThrowExceptionWhenTemplateBelongsToAnotherTenant(){

        // Arrange

        when(templateRepository.findByIdAndTenantId(eq(1L), anyLong()))
                .thenReturn(Optional.empty());


        // Act & Assert

        assertThrows(
                TemplateNotFoundException.class,
                () -> templateService.getTemplatesById(1L, tenant)
        );


        // Verify the call
        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));

    }



    // ------------ updateTemplateById ----------->

    @Test
    void shouldUpdateTemplateSuccessfully(){

        // Arrange

        Template response = new Template();
        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.of(response));

        when(templateRepository.existsByTenantIdAndNameAndIdNot(eq(tenant.getId()), eq(requestDto.getName()), eq(1L)))
                .thenReturn(false);

        when(templateRepository.saveAndFlush(any(Template.class)))
                .thenReturn(response);



        // Act

        TemplateResponseDto result = templateService.updateTemplateById(1L, requestDto, tenant);


        // Assert

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Subject", result.getSubject());
        assertEquals("Test Name", result.getName());
        assertEquals("Test Text Body", result.getBodyText());
        assertEquals("<h1> Test HTML Body </h1>", result.getBodyHtml());
        assertTrue(result.isActive());


        // Verify the call

        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
        verify(templateRepository).existsByTenantIdAndNameAndIdNot(eq(tenant.getId()), eq(requestDto.getName()), eq(1L));
        verify(templateRepository).saveAndFlush(any(Template.class));


    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentTemplate(){

        // Arrange

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.empty());


        // Act & Assert

        assertThrows(
                TemplateNotFoundException.class,
                () -> templateService.updateTemplateById(1L, requestDto, tenant)
        );


        // Verify the call

        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
        verify(templateRepository, never()).existsByTenantIdAndNameAndIdNot(eq(tenant.getId()), anyString(), eq(1L));
        verify(templateRepository, never()).saveAndFlush(any(Template.class));



    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateName(){

        // Arrange

        Template response = new Template();
        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.of(response));

        when(templateRepository.existsByTenantIdAndNameAndIdNot(eq(tenant.getId()), eq(requestDto.getName()), eq(1L)))
                .thenReturn(true);


        // Act

        assertThrows(
                TemplateNameAlreadyExistsException.class,
                () -> templateService.updateTemplateById(1L, requestDto, tenant)
        );


        // Verify the call

        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
        verify(templateRepository).existsByTenantIdAndNameAndIdNot(eq(tenant.getId()), eq(requestDto.getName()), eq(1L));
        verify(templateRepository, never()).saveAndFlush(any(Template.class));


    }

    @Test
    void shouldThrowExceptionWhenConcurrentDuplicateNameOccursDuringUpdate(){

        // Arrange

        Template response = new Template();
        response.setId(1L);
        response.setActive(true);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");

        TemplateRequestDto requestDto = new TemplateRequestDto();
        requestDto.setName("Test Name");
        requestDto.setSubject("Test Subject");
        requestDto.setBodyText("Test Text Body");
        requestDto.setBodyHtml("<h1> Test HTML Body </h1>");

        ConstraintViolationException constraintException = new ConstraintViolationException(
                "Duplicate template name", null, "uk_template_tenant_name");

        DataIntegrityViolationException databaseException = new DataIntegrityViolationException(
                "Database constraint violation", constraintException);

        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.of(response));

        when(templateRepository.existsByTenantIdAndNameAndIdNot(eq(tenant.getId()), anyString(), eq(1L)))
                .thenReturn(false);

        when(templateRepository.saveAndFlush(any(Template.class)))
                .thenThrow(databaseException);


        // Act & Assert

        assertThrows(TemplateNameAlreadyExistsException.class,
                () -> templateService.updateTemplateById(1L, requestDto, tenant));


        // Verify the call

        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
        verify(templateRepository).existsByTenantIdAndNameAndIdNot(eq(tenant.getId()), eq(requestDto.getName()), eq(1L));
        verify(templateRepository).saveAndFlush(any(Template.class));

    }



    // ----------- updateTemplateStatus ----------->

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldDeactivateActiveTemplateAndViceVersa(boolean active){

        // Arrange

        Template response = new Template();
        response.setId(1L);
        response.setActive(active);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");


        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.of(response));

        when(templateRepository.save(any(Template.class)))
                .thenReturn(response);

        // Act

        TemplateResponseDto result = templateService.updateTemplateStatus(1L, !active, tenant);

        // Assert

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(!active, result.isActive());

        // Verify the call

        verify(templateRepository, times(1)).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
        verify(templateRepository).save(any(Template.class));

    }



    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldThrowExceptionWhenTemplateIsAlreadyActiveOrIsAlreadyInactive(boolean active){

        // Arrange

        Template response = new Template();
        response.setId(1L);
        response.setActive(active);
        response.setSubject("Test Subject");
        response.setName("Test Name");
        response.setBodyText("Test Text Body");
        response.setBodyHtml("<h1> Test HTML Body </h1>");


        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.of(response));


        // Act & Assert

        if(active){
            assertThrows(
                    TemplateAlreadyActiveException.class,
                    () -> templateService.updateTemplateStatus(1L, active, tenant)
            );
        }else{
            assertThrows(
                    TemplateAlreadyInactiveException.class,
                    () -> templateService.updateTemplateStatus(1L, active, tenant)
            );
        }


        // Verify the call
        verify(templateRepository).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
        verify(templateRepository, never()).save(any(Template.class));

    }

    @Test
    void shouldThrowExceptionWhenUpdatingStatusOfNonExistentTemplate(){

        // Arrange

        when(templateRepository.findByIdAndTenantId(eq(1L), eq(tenant.getId())))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                TemplateNotFoundException.class,
                () -> templateService.updateTemplateStatus(1L, true, tenant)
        );


        // Verify the call

        verify(templateRepository, times(1)).findByIdAndTenantId(eq(1L), eq(tenant.getId()));
        verify(templateRepository, never()).save(any(Template.class));


    }
}
